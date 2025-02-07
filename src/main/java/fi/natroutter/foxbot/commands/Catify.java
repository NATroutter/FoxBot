package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.CatifyProvider;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Catify extends BaseCommand {

    private CatifyProvider catify = FoxBot.getCatify();

    public Catify() {
        super("catify");
        this.setDescription("Translate your text to cat language/puns");
        this.setHidden(true);
        this.addArguments(
                new OptionData(OptionType.STRING, "input", "Text you want to be translated!").setRequired(true)
        );
        this.setPermission(Nodes.CATIFY);
        this.setCooldownSeconds(120);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = FoxFrame.embedTemplate();

        eb.setAuthor("Here you go, MEOW!", null, bot.getAvatarUrl());
        eb.setDescription(catify(args.get(0).getAsString()));

        return eb;
    }

    private String catify(String input) {
        input = input.toLowerCase();

        for (Map.Entry<String, String> entry : catify.getReplacements().entrySet()) {
            String word = entry.getKey();
            String replacement = entry.getValue();

            Pattern wordPattern = Pattern.compile(word);
            Matcher matcher = wordPattern.matcher(input);
            input = matcher.replaceAll(replacement);
        }
        return input;
    }
}
