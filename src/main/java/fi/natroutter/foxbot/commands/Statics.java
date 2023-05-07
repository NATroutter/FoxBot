package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.concurrent.Task;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class Statics extends BaseCommand {

    public Statics() {
        super("statics");
        this.setDescription("Guild statistics!");
        this.setPermission(Node.STATICS);
        this.setHidden(false);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = Utils.embedBase();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        eb.setTitle("Guild Statistics");
        eb.setThumbnail(guild.getIconUrl());

        if (guild.getDescription() != null) {
            eb.setDescription(
                    "\uD83D\uDD16 **Name:**\n_" + guild.getName() +"_"+
                            "\n\n" +
                            "\uD83D\uDCD6 **Description:**\n_" + guild.getDescription() + "_" +
                            "\n\n" +
                            "\uD83D\uDC82 **Explicit Level:**\n_" + guild.getExplicitContentLevel().getDescription() + "_" +
                            "\n\u200E"
            );
        }

        if (guild.getOwner() != null) {
            eb.addField("\uD83D\uDC51 Owner:", "_"+guild.getOwner().getUser().getAsTag()+"_", true);
        }
        eb.addField("\uD83D\uDCC7 Server ID:", "_"+guild.getId()+"_", true);
        eb.addField("\uD83D\uDD1E NSWF Level:", "_"+formatNswfLevel(guild.getNSFWLevel())+"_", true);
        eb.addField("\uD83D\uDD25 Nitro boosters:", "_"+guild.getBoostCount()+"_", true);
        eb.addField("\uD83D\uDCA5 Boost Tier:", "_"+formatBoostTier(guild.getBoostTier())+"_", true);
        eb.addField("\uD83D\uDD52 Creation Date:", "_"+guild.getTimeCreated().format(formatter)+"_", true);

        eb.addField("⌛ AFK Timeout:", "_"+guild.getAfkTimeout().getSeconds()+"_",true);
        eb.addField("☎️ MFA Level:", "_"+formatMFA(guild.getRequiredMFALevel())+"_",true);

        eb.addField("\uD83D\uDD11 Verify Level:", "_"+formatVerificationLevel(guild.getVerificationLevel())+"_",true);

        eb.addField("\uD83D\uDCDD Total Members:", "_"+guild.getMemberCount()+"_", true);

        Task<List<Member>> members = guild.loadMembers();

        members.onSuccess(list-> {
            int online = 0, offline = 0, disturb = 0, idle = 0;

            for(Member m : list) {
                switch (m.getOnlineStatus()) {
                    case ONLINE -> ++online;
                    case OFFLINE -> ++offline;
                    case DO_NOT_DISTURB -> ++disturb;
                    case IDLE -> ++idle;
                }
            }

            eb.addField("\uD83D\uDFE2 Online", "_"+online+"_", true);
            eb.addField("⚫  Offline", "_"+offline+"_", true);
            eb.addField("\uD83D\uDD34 Dnd", "_"+disturb+"_", true);
            eb.addField("\uD83D\uDFE1 Idle", "_"+idle+"_", true);
        });
        return eb;
    }

    private String formatBoostTier(Guild.BoostTier tier) {
        return switch (tier) {
            case UNKNOWN -> "Unknown";
            case NONE -> "None";
            case TIER_1 -> "Tier 1";
            case TIER_2 -> "Tier 2";
            case TIER_3 -> "Tier 3";
        };
    }

    private String formatNswfLevel(Guild.NSFWLevel level) {
        return switch (level) {
            case UNKNOWN -> "Unknown";
            case SAFE -> "Safe";
            case DEFAULT -> "Default";
            case EXPLICIT -> "Explicit";
            case AGE_RESTRICTED -> "Age restricted";
        };
    }

    private String formatVerificationLevel(Guild.VerificationLevel level) {
        return switch (level) {
            case UNKNOWN -> "Unknonw";
            case NONE -> "None";
            case LOW -> "Low";
            case HIGH -> "High";
            case MEDIUM -> "Medium";
            case VERY_HIGH -> "Very High";
        };
    }

    private String formatMFA(Guild.MFALevel level){
        return switch (level) {
            case NONE -> "None";
            case TWO_FACTOR_AUTH -> "2FA";
            case UNKNOWN -> "Unknown";
        };
    }
}
