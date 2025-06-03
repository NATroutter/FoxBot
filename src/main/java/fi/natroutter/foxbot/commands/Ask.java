package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.objects.GifData;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
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

    private ConfigProvider config = FoxBot.getConfig();
    private FoxLogger logger = FoxBot.getLogger();

    public Ask() {
        super("ask");
        this.setDescription("Ask yes/no questions?");
        this.setHidden(false);
        this.setPermission(Nodes.ASK);
        this.addArguments(
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
    public Object onCommand(JDA jda, Member member, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = FoxFrame.embedTemplate();

//        TextChannel chan = guild.getTextChannelById(527849417957441548L);
//        chan.sendMessageEmbeds(error("This command is disabled!").build()).queue();
//        chan.sendMessageEmbeds(info("This action is ready to go").build()).queue();
//        chan.sendMessageEmbeds(usage("You need to provide more arguments", "/ask <arg> <arg2>").build()).queue();

        User user = member.getUser();
        String avatar = user.getAvatar() != null ? user.getAvatar().getUrl(512) : user.getAvatarUrl();

        eb.setAuthor(member.getUser().getName() + " asked question!", null, avatar);

        OptionMapping simplify = getOption(args, "simplify");
        boolean maybe = simplify != null && simplify.getAsBoolean();

        Answare answer = Answare.random(maybe);
        String answerEmoji = answer == Answare.YES ? "✅" : answer == Answare.NO ? "❌" : "❔";

        Random rnd = new Random();
        String answerText = answer != Answare.MAYBE ? (rnd.nextBoolean() ? "Definetly " : "") + answer.getFriendly().toLowerCase() : answer.getFriendly();

        eb.addField("\uD83D\uDC81 Question:", getOption(args, "question").getAsString(), false);
        eb.addField(answerEmoji + " Answer:", answerText, false);
        logger.info(member.getUser().getGlobalName() + " asked question: (" + getOption(args, "question").getAsString() + ") and got answer: " + answerText);
        try {
            String json = Jsoup.connect(endPoint(answer)).ignoreContentType(true).userAgent("FoxBot/1.0 (NATroutter)").execute().body();
            GifData gif = new Gson().fromJson(json, GifData.class);
            eb.setImage(gif.data.images.original.url);
        } catch (Exception e) {
            logger.error("Failed to get gif from giphy");
            e.printStackTrace();
            return error("Failed to answer question!");
        }

        return eb;
    }
}
