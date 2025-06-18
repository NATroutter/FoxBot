package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.CatifyProvider;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Catify extends DiscordCommand {

    private CatifyProvider catify = FoxBot.getCatify();

    public Catify() {
        super("catify");
        this.setDescription("Translate your text to cat speech");
        this.setPermission(Nodes.CATIFY);
        this.setCooldownSeconds(120);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "input", "Text you want to be translated!").setRequired(true)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        User bot = event.getJDA().getSelfUser();
        String input = Objects.requireNonNull(event.getOption("input")).getAsString();

        EmbedBuilder eb = FoxFrame.embedTemplate();

        eb.setAuthor("Here you go, MEOW!", null, bot.getAvatarUrl());
        eb.setDescription(catify(input));

        reply(event, eb);
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
