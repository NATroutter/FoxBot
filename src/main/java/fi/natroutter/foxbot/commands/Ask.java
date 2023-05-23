package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.Config;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.objects.GIfData;
import fi.natroutter.foxbot.objects.Posts;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Jsoup;

import java.util.List;
import java.util.Random;

public class Ask extends BaseCommand {

    private Config config = FoxBot.getConfig().get();
    private NATLogger logger = FoxBot.getLogger();

    public Ask() {
        super("ask");
        this.setDescription("Ask yes/no questions?");
        this.setHidden(false);
        this.setPermission(Node.ASK);
        this.addArguments(
                new OptionData(OptionType.STRING, "question", "What you want to get know?").setRequired(true)
        );
    }

    @AllArgsConstructor @Getter
    private enum Answare {
        YES("yes", "Yes"),
        NO("no", "No"),
        MAYBE("maybe", "Maybe");
        private final String tag;
        private final String friendly;

        public static Answare random()  {
            Random rnd = new Random();
            Answare[] answares = values();
            return answares[rnd.nextInt(answares.length)];
        }
    }

    private String endPoint(Answare answare) {
        return "http://api.giphy.com/v1/gifs/random?api_key=" +config.getApiKeys().getGiphy()+ "&tag=" + answare.getTag();
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = Utils.embedBase();

        User user = member.getUser();
        String avatar = user.getAvatar() != null ? user.getAvatar().getUrl(512) : user.getAvatarUrl();

        eb.setAuthor(member.getUser().getName() + " asked question!", null, avatar);

        Answare answer = Answare.random();
        String answerEmoji = answer == Answare.YES ? "✅" : answer == Answare.NO ? "❌" : "❔";

        Random rnd = new Random();
        String answerText = answer != Answare.MAYBE ? (rnd.nextBoolean() ? "Definetly " : "") + answer.getFriendly().toLowerCase() : answer.getFriendly();

        eb.addField("\uD83D\uDC81 Question:", getOption(args, "question").getAsString(), false);
        eb.addField(answerEmoji + " Answer:", answerText, false);
        logger.info(member.getUser().getAsTag() + " asked question: (" + getOption(args, "question").getAsString() + ") and got answer: " + answerText);
        try {
            String json = Jsoup.connect(endPoint(answer)).ignoreContentType(true).userAgent("FoxBot/1.0 (NATroutter)").execute().body();
            GIfData gif = new Gson().fromJson(json, GIfData.class);
            eb.setImage(gif.data.images.original.url);
        } catch (Exception e) {
            logger.error("Failed to get gif from giphy");
            e.printStackTrace();
            return error("Failed to answer question!");
        }

        return eb;
    }
}
