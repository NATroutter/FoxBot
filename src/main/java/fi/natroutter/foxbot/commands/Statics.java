package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class Statics extends BaseCommand {

    public Statics() {
        super("statics");
        this.setDescription("Guild statistics!");
        this.setPermission(Nodes.STATICS);
        this.setHidden(false);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = Utils.embedBase();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        eb.setAuthor("📈 Server Info 📊");
        eb.setThumbnail(guild.getIconUrl());
        eb.addField("Name", guild.getName(), true);
        eb.addBlankField(true);
        if (guild.getOwner() != null) {
            eb.addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator() , true);
        }
        eb.addField("Server ID", guild.getId(), true);
        eb.addField("NSWFLevel", guild.getNSFWLevel().name(), true);
        eb.addField("Nitro boosters", String.valueOf(guild.getBoostCount()), true);
        eb.addField("Boost Tier", guild.getBoostTier().name(), true);
        eb.addField("Creation Date", guild.getTimeCreated().format(formatter), true);
        eb.addField("Total Members", String.valueOf(guild.getMemberCount()), true);

        int online = 0, offline = 0, disturb = 0, idle = 0;
        for(Member m : guild.getMembers()) {
            switch (m.getOnlineStatus()) {
                case ONLINE -> ++online;
                case OFFLINE -> ++offline;
                case DO_NOT_DISTURB -> ++disturb;
                case IDLE -> ++idle;
            }
        }
        eb.addField("Members Online", String.valueOf(online), true);
        eb.addField("Members Offline", String.valueOf(offline), true);
        eb.addField("Members Dnd", String.valueOf(disturb), true);
        eb.addField("Members Idle", String.valueOf(idle), true);


        return eb;
    }
}
