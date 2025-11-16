package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Pick extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();

    //TODO add "public" option (required:false) - Determines is the resulting message hidden or not!

    public Pick() {
        super("pick");
        this.setDescription("Pick random answer to your question!");
        this.setPermission(Nodes.PICK);
    }

    private final List<String> answerTitles = Arrays.asList(
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

    record answerOption(String title, String answer) {}

    private List<answerOption> getAnswerList(SlashCommandInteractionEvent event) {
        List<answerOption> answerList = new ArrayList<>();
        for (int i = 1; i < 25; i++) {
            OptionMapping arg = event.getOption("answer" + i);
            if (arg != null) {
                answerList.add(new answerOption("**"+i+".**", arg.getAsString()));
            }
        }
        return answerList;
    }

    @Override
    public List<OptionData> options() {
        List<OptionData> data = new ArrayList<>();
        data.add(new OptionData(OptionType.STRING, "question", "What you want to get know?").setRequired(true));
        for (int i = 1; i < 25; i++) {
            data.add(
                    new OptionData(OptionType.STRING, "answer"+i, "Select answer #"+i)
                            .setRequired(i == 1 || i == 2)
            );
        }
        return data;
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = FoxFrame.embedTemplate();

        String question = Objects.requireNonNull(event.getOption("question")).getAsString();

        List<answerOption> answerList = getAnswerList(event);
        if (answerList.isEmpty() || answerList.size() < 2) {
            replyError(event, "You need to provide at least 2 answers!");
            return;
        }

        SecureRandom rnd = new SecureRandom();

        Member member = event.getMember();
        User user = member.getUser();
        String avatar = user.getAvatar() != null ? user.getAvatar().getUrl(512) : user.getAvatarUrl();

        String optionsList = answerList.stream().map(opt-> opt.title()+ " *"+ opt.answer() + "*").collect(Collectors.joining("\n"));
        answerOption answerText = answerList.get(rnd.nextInt(answerList.size()));

        eb.setAuthor(user.getName() + " asked question!", null, avatar);
        eb.setDescription(
                "\uD83D\uDC81 **Question:**\n"+
                "*"+question+"*\n\n" +
                "\uD83D\uDCDA **Options:**\n" +
                optionsList + "\n\u200E"
        );

        eb.addField("\uD83D\uDCD6 **" + answerTitles.get(rnd.nextInt(answerTitles.size())) + "**", answerText.title() + " *" + answerText.answer() + "*", false);

        logger.info(user.getGlobalName() + " asked question (" + question + ") and got answer: " + answerText.answer());

        reply(event, eb);
    }

}
