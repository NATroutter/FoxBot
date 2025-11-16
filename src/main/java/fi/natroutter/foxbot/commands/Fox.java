package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class Fox extends DiscordCommand {

    public Fox() {
        super("fox");
        this.setDescription("Post random cute and fluffy fox pictures ❤️");
        this.setPermission(Nodes.FOX);
        this.setCooldownTime(120);
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setImage(Utils.randomFox());
        reply(event, eb);
    }
}
