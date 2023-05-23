package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.handlers.permissions.Permissions;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.NATLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.ImageProxy;
import net.dv8tion.jda.api.utils.concurrent.Task;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Info extends BaseCommand {

    private NATLogger logger = FoxBot.getLogger();

    public Info() {
        super("Info");
        this.setDescription("User information!");
        this.setHidden(false);

        this.addArguments(
                new OptionData(OptionType.STRING, "type", "What kind of information you want to know")
                        .setRequired(true)
                        .addChoice("server-info", "server-info")
                        .addChoice("role-info", "role-info")
                        .addChoice("user-info", "user-info"),
                new OptionData(OptionType.USER, "user", "User that you want to see info")
                        .setRequired(false),
                new OptionData(OptionType.ROLE, "role", "Role that you want to see info")
                        .setRequired(false)
        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = Utils.embedBase();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        String type = getOption(args, "type").getAsString();
        if (args.size() == 0) {
            return error("You need to select what type of information you want to get!");
        } else if (args.size() == 1) {
            switch (type.toLowerCase()) {
                case "server-info": {
                    try {
                        if (Permissions.has(member, Node.INFO_SERVER).get(5, TimeUnit.SECONDS)) {
                            return serverInfo(guild, bot);
                        }
                    } catch (Exception e) {
                        return error("Failed to check permissions!");
                    }
                    return error("You don't have permission to use this command!");
                }
                case "role-info": return error("You need to select a role that you want to see more information!");
                case "user-info": return error("You need to select a user that you want to see more information!");
                default: return error("Invalid selection!");
            }
        } else if (args.size() == 2) {
            switch (type.toLowerCase()) {
                case "server-info": return error("Too many arguments!");
                case "role-info": {
                    OptionMapping roleOpt = getOption(args, "role");
                    if (roleOpt == null) {return error("Role is not defined!");}
                    Role role = roleOpt.getAsRole();

                    try {
                        if (Permissions.has(member, Node.INFO_ROLE).get(5, TimeUnit.SECONDS)) {
                            return roleInfo(guild, role, bot);
                        }
                    } catch (Exception e) {
                        return error("Failed to check permissions!");
                    }
                    return error("You don't have permission to use this command!");
                }
                case "user-info": {
                    OptionMapping roleOpt = getOption(args, "user");
                    if (roleOpt == null) {return error("Role is not defined!");}
                    User user = roleOpt.getAsUser();

                    try {
                        if (Permissions.has(member, Node.INFO_USER).get(5, TimeUnit.SECONDS)) {
                            return userInfo(guild, user, bot);
                        }
                    } catch (Exception e) {
                        return error("Failed to check permissions!");
                    }
                    return error("You don't have permission to use this command!");
                }
                default: return error("Invalid selection!");
            }
        } else {
            return error("Too many arguments!");
        }
    }

    private EmbedBuilder userInfo(Guild guild, User user, User bot) {
        EmbedBuilder eb = Utils.embedBase();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        Member member = guild.getMemberById(user.getIdLong());
        String nickname = member != null && member.getNickname() != null ? member.getNickname() : user.getName();
        String joinDate = member != null ? member.getTimeJoined().format(formatter) : "Unknown";
        String Perms = member != null ? member.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")) : "Unknown";
        String roles = member != null ? member.getRoles().stream().map(IMentionable::getAsMention).collect(Collectors.joining(", ")) : "Unknown";
        int roleCount = member != null ? member.getRoles().size() : 0;
        String avatar = user.getAvatar() != null ? user.getAvatar().getUrl(512) : user.getAvatarUrl();


        eb.setAuthor("User Information", bot.getAvatar() != null ? bot.getAvatar().getUrl(512) : bot.getAvatarUrl());
        eb.setTitle(user.getName());

        eb.setThumbnail(avatar);

        eb.addField("\uD83D\uDCDB Username:", "_"+user.getName()+"_", true);
        eb.addField("\uD83C\uDFF7 Nickname:", "_"+nickname+"_", true);
        eb.addField("\uD83D\uDD16 Discriminator:", "_"+user.getDiscriminator()+"_", true);

        eb.addField("⏰ Account Created:", "_"+user.getTimeCreated().format(formatter)+"_", true);
        eb.addField("⏰ Guild Join:", "_"+joinDate+"_", true);

        eb.addField("\uD83E\uDEAA User ID:", "_"+user.getId()+"_", true);
        eb.addField("\uD83E\uDD16 IsBot:", "_"+(user.isBot() ? "Yes" : "No")+"_", true);

        try {
            User.Profile profile = user.retrieveProfile().submit().get(5, TimeUnit.SECONDS);
            String banner = profile.getBanner() != null ? profile.getBanner().getUrl(512) : profile.getBannerUrl();

            eb.addField("\uD83D\uDDBC Profile Images:" , "_[Avatar]("+avatar+")\n[Banner]("+banner+")_",true);
            eb.setImage(banner);

            Color ac = profile.getAccentColor();
            String color = ac != null ? "(RGB: "+ac.getRed()+","+ac.getGreen()+","+ac.getBlue()+")" : "Unknown";

            eb.addField("\uD83D\uDD8D AccentColor:", "_" + color + "_",true);
        } catch (Exception e) {
            logger.error("Failed to retreive user profile for " + user.getAsTag() + "("+user.getId()+")");
            eb.addField("\uD83D\uDDBC Profile Images:" , "_[Avatar]("+avatar+")_",true);
            eb.addField("\uD83D\uDD8D AccentColor:", "_Unknown_",true);
        }

        eb.addField("\uD83D\uDDD2 Permissions", "_"+Perms+"_", false);
        eb.addField("\uD83D\uDC6A Roles (" +(roleCount > 0 ? roleCount : "?" )+ "):", "_"+roles+"_", false);

        return eb;
    }
    private EmbedBuilder roleInfo(Guild guild, Role role, User bot) {
        EmbedBuilder eb = Utils.embedBase();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        eb.setAuthor("Role Information", bot.getAvatarUrl());
        eb.setTitle(role.getName());

        int members = guild.getMembersWithRoles(role).size();

        String Perms = role.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "));

        String icon = "_Unknown_";
        if (role.getIcon() != null) {
            icon = role.getIcon().getIcon() != null ? role.getIcon().getIcon().getUrl(512) : role.getIcon().getIconUrl();
            if (icon != null && !icon.isEmpty()) {
                eb.setThumbnail(icon);
                icon = "_[Icon]("+icon+")_";
            }
        }

        Color ac = role.getColor();
        String color = ac != null ? "(RGB: "+ac.getRed()+","+ac.getGreen()+","+ac.getBlue()+")" : "Unknown";

        eb.addField("\uD83E\uDEAA Role ID:", "_" + role.getId() + "_", true);
        eb.addField("\uD83D\uDCCB Total Members:", "_" + members + "_", true);
        eb.addField("\uD83D\uDD8D Color:", "_" + color + "_", true);
        eb.addField("\uD83D\uDDBC Image:", icon, true);

        eb.addField("⏰ Creation date:", role.getTimeCreated().format(formatter), false);
        eb.addField("\uD83D\uDDD2 Permissions:", Perms, false);

        return eb;
    }

    private EmbedBuilder serverInfo(Guild guild, User bot) {
        EmbedBuilder eb = Utils.embedBase();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        eb.setAuthor("Server Information", bot.getAvatar() != null ? bot.getAvatar().getUrl(512) : bot.getAvatarUrl());
        eb.setTitle(guild.getName());
        eb.setThumbnail(guild.getIcon() != null ? guild.getIcon().getUrl(512) : guild.getIconUrl());

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

            eb.addField("\uD83D\uDFE2 Online:", "_"+online+"_", false);
            eb.addField("⚫  Offline:", "_"+offline+"_", false);
            eb.addField("\uD83D\uDD34 Dnd:", "_"+disturb+"_", false);
            eb.addField("\uD83D\uDFE1 Idle:", "_"+idle+"_", false);
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
            case UNKNOWN -> "Unknown";
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
