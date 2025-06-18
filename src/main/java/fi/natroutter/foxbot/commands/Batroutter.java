package fi.natroutter.foxbot.commands;

import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Batroutter extends DiscordCommand {

    public Batroutter() {
        super("batroutter");
        this.setDescription("Its a mystery!");
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Hmmm...");
        eb.setDescription("\uD83E\uDD87 Jollain Taitaa olla lepakoita vintillä \uD83D\uDE09 \uD83E\uDD87 ");
        eb.setThumbnail("https://i.imgur.com/8ZVuw6p.jpg");

        reply(event, eb);
    }
}
