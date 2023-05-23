package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.Config;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.objects.GIfData;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Pick extends BaseCommand {

    private NATLogger logger = FoxBot.getLogger();

    List<String> answerTitles = Arrays.asList(
            "I think this is the best option:",
            "This is today's special:",
            "This is the only good option:",
            "All of this options are shit but this is the least:",
            "I wouldn't choose any of these, but if you have to know i would choose this:",
            "In my opinion, this is the top choice:",
            "Look no further, this is the perfect option:",
            "Today's standout selection is:",
            "While none of these are ideal, this is the most suitable:",
            "If I had to choose, this would be my recommendation:",
            "Without a doubt, this is the winner:",
            "Consider this as your best bet:",
            "This option stands out from the rest:",
            "Among the mediocre choices, this one shines:",
            "When it comes down to it, this is the safest pick:",
            "Out of all these, this is the most promising:",
            "Despite the lackluster options, this is the preferable one:",
            "I'm not thrilled with any of these, but this is the least disappointing:",
            "Though it's far from perfect, this is the best compromise:",
            "While none are outstanding, this option is relatively acceptable:"
    );

    public Pick() {
        super("pick");
        this.setDescription("Pick random answer to your question!");
        this.setHidden(false);
        this.setPermission(Node.PICK);
        this.addArguments(
                new OptionData(OptionType.STRING, "question", "What you want to get know?").setRequired(true),
                new OptionData(OptionType.STRING, "answer1", "Select answer #1").setRequired(false),
                new OptionData(OptionType.STRING, "answer2", "Select answer #2").setRequired(false),
                new OptionData(OptionType.STRING, "answer3", "Select answer #3").setRequired(false),
                new OptionData(OptionType.STRING, "answer4", "Select answer #4").setRequired(false),
                new OptionData(OptionType.STRING, "answer5", "Select answer #5").setRequired(false),
                new OptionData(OptionType.STRING, "answer6", "Select answer #6").setRequired(false),
                new OptionData(OptionType.STRING, "answer7", "Select answer #7").setRequired(false),
                new OptionData(OptionType.STRING, "answer8", "Select answer #7").setRequired(false),
                new OptionData(OptionType.STRING, "answer9", "Select answer #7").setRequired(false),
                new OptionData(OptionType.STRING, "answer10", "Select answer #8").setRequired(false)
        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = Utils.embedBase();

        List<answerOption> answerList = getAnswerList(args);
        if (answerList == null || answerList.isEmpty() || answerList.size() < 2) {
            return error("You need to provide at least 2 answers!");
        }

        Random rnd = new Random();

        User user = member.getUser();
        String avatar = user.getAvatar() != null ? user.getAvatar().getUrl(512) : user.getAvatarUrl();

        String optionsList = getAnswerList(args).stream().map(opt-> opt.emoji()+ " _"+ opt.answer() + "_").collect(Collectors.joining("\n"));
        answerOption answerText = getAnswerList(args).get(rnd.nextInt(getAnswerList(args).size()));

        eb.setAuthor(member.getUser().getName() + " asked question!", null, avatar);
        eb.setDescription(
                "\uD83D\uDC81 **Question:**\n"+
                "_"+getOption(args, "question").getAsString()+"_\n\n" +
                "\uD83D\uDCDA **Options:**\n" +
                optionsList + "\n\u200E"
        );

        eb.addField("\uD83D\uDCD6 **" + answerTitles.get(rnd.nextInt(answerTitles.size())) + "**", answerText.emoji() + " _" + answerText.answer() + "_", false);

        logger.info(member.getUser().getAsTag() + " asked question (" + getOption(args, "question").getAsString() + ") and got answer: " + answerText.answer());

        return eb;
    }

    record answerOption(String emoji, String answer) {}
    private List<answerOption> getAnswerList(List<OptionMapping> args) {
        List<answerOption> answerList = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            OptionMapping arg = getOption(args, "answer" + i);
            if (arg != null) {
                answerList.add(new answerOption(numToEmoji(i), arg.getAsString()));
            }
        }
        return answerList;
    }

    private String numToEmoji(int value) {
        switch (value) {
            case 1: return "**1.**";
            case 2: return "**2.**";
            case 3: return "**3.**";
            case 4: return "**4.**";
            case 5: return "**5.**";
            case 6: return "**6.**";
            case 7: return "**7.**";
            case 8: return "**8.**";
            case 9: return "**9.**";
            case 10: return "**10.**";
            default: return "";
        }
    }

}
