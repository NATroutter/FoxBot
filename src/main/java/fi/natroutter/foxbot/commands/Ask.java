package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.data.GiphyData;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Jsoup;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Ask extends DiscordCommand {

    //TODO add "public" option (required:false) - Determines is the resulting message hidden or not!

    private ConfigProvider config = FoxBot.getConfig();
    private FoxLogger logger = FoxBot.getLogger();

    public Ask() {
        super("ask");
        this.setDescription("Ask yes/no questions?");
        this.setPermission(Nodes.ASK);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "question", "What you want to get know?").setRequired(true),
                new OptionData(OptionType.BOOLEAN, "simplify", "Do you want to exclude maybe answer?").setRequired(false)
        );
    }

    @AllArgsConstructor @Getter
    private enum Answare {
        YES("yes", "Yes"),
        NO("no", "No"),
        MAYBE("maybe", "Maybe");
        private final String tag;
        private final String friendly;

        public static Answare random(boolean simplify)  {
            Random rnd = new Random();
            Answare[] answares = values();
            return answares[rnd.nextInt(answares.length - (simplify ? 1 : 0))];
        }
    }

    private String endPoint(Answare answare) {
        return "http://api.giphy.com/v1/gifs/random?api_key=" +config.get().getApiKeys().getGiphy()+ "&tag=" + answare.getTag();
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Member member = event.getMember();

        String question = Objects.requireNonNull(event.getOption("question")).getAsString();

        OptionMapping simplify = event.getOption("simplify");
        boolean maybe = simplify != null && simplify.getAsBoolean();

        EmbedBuilder eb = FoxFrame.embedTemplate();

        User user = member.getUser();
        String avatar = user.getAvatar() != null ? user.getAvatar().getUrl(512) : user.getAvatarUrl();

        eb.setAuthor(user.getName() + " asked question!", null, avatar);

        Answare answer = Answare.random(maybe);
        String answerEmoji = answer == Answare.YES ? "✅" : answer == Answare.NO ? "❌" : "❔";

        Random rnd = new Random();
        String answerText = answer != Answare.MAYBE ? (rnd.nextBoolean() ? "Definetly " : "") + answer.getFriendly().toLowerCase() : answer.getFriendly();

        eb.addField("\uD83D\uDC81 Question:", question, false);
        eb.addField(answerEmoji + " Answer:", answerText, false);
        logger.info(user.getGlobalName() + " asked question: (" + question + ") and got answer: " + answerText);
        try {
            String json = Jsoup.connect(endPoint(answer)).ignoreContentType(true).userAgent("FoxBot/1.0 (NATroutter)").execute().body();
            GiphyData gif = new Gson().fromJson(json, GiphyData.class);
            eb.setImage(gif.data.images.original.url);
        } catch (Exception e) {
            logger.error("Failed to get gif from giphy");
            e.printStackTrace();
            errorMessage(event,"Failed to answer question!");
            return;
        }
        reply(event, eb, false);
    }
}
