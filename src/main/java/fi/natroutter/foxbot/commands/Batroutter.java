package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

public class Batroutter extends BaseCommand {

    public Batroutter() {
        super("batroutter");
        this.setDescription("Its a mystery!");
        this.setHidden(false);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Hmmm...");
        eb.setDescription("\uD83E\uDD87 Jollain Taitaa olla lepakoita vintillä \uD83D\uDE09 \uD83E\uDD87 ");
        eb.setThumbnail("https://i.imgur.com/8ZVuw6p.jpg");

        return eb;
    }
}
