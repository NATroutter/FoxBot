package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class Shorten extends DiscordCommand {

    private final FoxLogger logger = FoxBot.getLogger();
    private final Config config = FoxBot.getConfigProvider().get();

    private final Gson gson = new Gson();

    public Shorten() {
        super("shorten");
        this.setDescription("Shorten urls");
        this.setPermission(Nodes.SHORTER);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "url", "Original url to shorten")
                        .setRequired(true),
                new OptionData(OptionType.INTEGER, "max-views", "Maximum amount of views allowed")
                        .setRequired(false),
                new OptionData(OptionType.STRING, "password", "password for accessing the url")
                        .setRequired(false),
                new OptionData(OptionType.STRING, "vanity", "Vanity string for the url")
                        .setRequired(false)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        String url = Objects.requireNonNull(event.getOption("url")).getAsString();

        OptionMapping maxViewOption = event.getOption("max-views");
        OptionMapping vanityOption  = event.getOption("vanity");
        OptionMapping passwordOption  = event.getOption("password");

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            replyError(event, "Invalid URL format. Please provide a complete URL starting with http:// or https://");
            return;
        }

        // Build JSON object with Gson
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("destination", url);

        if (vanityOption != null && !FoxLib.isBlank(vanityOption.getAsString())) {
            requestBody.addProperty("vanity", vanityOption.getAsString());
        }

        //construct the base connection for the shorter api
        Connection connect = Jsoup.connect(config.getZipline().getEndpoint() + "/api/user/urls")
                .method(Connection.Method.POST)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .header("Content-Type", "application/json")
                .header("Authorization", config.getZipline().getToken())
                .userAgent("FoxBot/1.0 (NATroutter)")
                .requestBody("{\"destination\": \""+url+"\"}")
                .timeout(5000)
                ;

        //Add additional headers depending on the options
        if (maxViewOption != null && !FoxLib.isBlank(maxViewOption.getAsString())) {
            connect.header("x-zipline-max-views", maxViewOption.getAsString());
        }
        if (passwordOption != null && !FoxLib.isBlank(passwordOption.getAsString())) {
            connect.header("x-zipline-password", passwordOption.getAsString());
        }

        try {
            Connection.Response response = connect.execute();

            String body = response.body();
            int statusCode = response.statusCode();

            JsonObject json = gson.fromJson(body, JsonObject.class);

            if (statusCode != 200) {
                String error = json.has("error") ? json.get("error").getAsString() : response.body();
                replyError(event, "API Error!", error);
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("**URL:** ").append(json.get("url").getAsString());
            if (passwordOption != null && !FoxLib.isBlank(passwordOption.getAsString())) {
                sb.append("\n**Password:** ").append(passwordOption.getAsString());
            }
            if (maxViewOption != null && !FoxLib.isBlank(maxViewOption.getAsString())) {
                sb.append("\n**Max Views:** ").append(maxViewOption.getAsString());
            }

            replySuccess(event, "Url has been shortened!", sb.toString(), true, false);


        } catch (IOException e) {
            logger.error("Failed to shorten url : " + e.getMessage());
            replyError(event, "Failed to shorten URL!");
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse API response: " + e.getMessage());
            replyError(event, "Received invalid response from API!");
        }
    }

}
