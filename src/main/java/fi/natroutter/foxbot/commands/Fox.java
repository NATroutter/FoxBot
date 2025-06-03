package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

public class Fox extends BaseCommand {

    public Fox() {
        super("fox");
        this.setDescription("Post random cute and fluffy fox pictures ❤️");
        this.setPermission(Nodes.FOX);
        this.setCooldownSeconds(120);
        this.setHidden(false);
    }

    @Override
    public Object onCommand(JDA jda, Member member, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setImage(Utils.randomFox());
        return eb;
    }
}
