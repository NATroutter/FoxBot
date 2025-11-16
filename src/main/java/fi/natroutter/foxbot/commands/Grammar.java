package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.AIRequestProvider;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.data.AIRequest;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.data.AIResponse;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class Grammar extends DiscordCommand {

    private Gson gson = new Gson();
    private ConfigProvider config = FoxBot.getConfigProvider();
    private AIRequestProvider aiRequest = FoxBot.getAiRequestProvider();
    private FoxLogger logger = FoxBot.getLogger();

    private AIRequest request;
    private Config.OpenAI grammar;

    public Grammar() {
        super("grammar");
        this.setDescription("Fix the grammar and spelling mistakes on inputted text");
        this.setPermission(Nodes.GRAMMAR);
        this.setCooldownTime(120);

        request = aiRequest.get("grammar");
        grammar = config.get().getOpenAI().stream().filter(ai->ai.getName().equalsIgnoreCase("grammar")).findFirst().orElse(null);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "text", "Text to fix").setRequired(true)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Member member = event.getMember();

        if (grammar == null) {
            replyError(event, "configuration key \"grammar\" is missing from OpenAI configuration in config.json");
            return;
        }
        String text = Objects.requireNonNull(event.getOption("text")).getAsString();
        request.setInput(text);
        request.setMax_tool_calls(1);

        event.replyEmbeds(grammarLoading())
                .setEphemeral(true)
                .queue((message->{
                    String output = fixGrammar(request);
                    if (output == null) {
                        message.editOriginalEmbeds(FoxFrame.error("Failed to fix grammar, error occurred in request!").build()).queue();
                        return;
                    }
                    message.editOriginalEmbeds(grammarLoaded(output)).queue();
                }));
    }

    @SneakyThrows
    private String fixGrammar(AIRequest request) {
        Connection.Response response = Jsoup.connect(grammar.getEndpoint())
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .method(Connection.Method.POST)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + grammar.getApikey())
                .requestBody(gson.toJson(request))
                .userAgent("FoxBot/1.0 (NATroutter)")
                .execute();

        if (response.statusCode() != 200) {
            logger.error("Failed to fix grammar for user input",
                    new LogData("Body", response.body())
            );
            return null;
        }

        AIResponse resp = new Gson().fromJson(response.body(), AIResponse.class);
        return resp.getOutput().getFirst().getContent().getFirst().getText();
    }

    public MessageEmbed grammarLoading() {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(new Color(166, 36, 36));
        eb.setTitle("Grammar Correction");
        eb.setDescription("_Your text is currently being processed, Please standby_");
        eb.setThumbnail("https://cdn.nat.gg/img/css-loader.gif");
        return eb.build();
    }
    public MessageEmbed grammarLoaded(String text) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(new Color(67, 160, 71));
        eb.setTitle("Grammar Correction");
        eb.setDescription("_"+text+"_");
        eb.setThumbnail("https://cdn.nat.gg/img/green_checkmark.png");
        return eb.build();
    }

}
