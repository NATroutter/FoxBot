package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
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
        this.setPermission(Node.FOX);
        this.setCooldownSeconds(120);
        this.setHidden(false);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = Utils.embedBase();
        eb.setImage(Utils.randomFox());
        return eb;
    }
}
