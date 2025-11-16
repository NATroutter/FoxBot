package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.concurrent.Task;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Info extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public Info() {
        super("Info");
        this.setDescription("Get Information about server,roles and members!");
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "type", "What kind of information you want to know")
                        .setRequired(true)
                        .addChoice("server", "server")
                        .addChoice("roles", "roles")
                        .addChoice("users", "users"),
                new OptionData(OptionType.USER, "user", "User that you want to see info")
                        .setRequired(false),
                new OptionData(OptionType.ROLE, "role", "Role that you want to see info")
                        .setRequired(false)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        User bot = event.getJDA().getSelfUser();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        String type = Objects.requireNonNull(event.getOption("type")).getAsString();

        switch (type.toLowerCase()) {
            case "server" -> {
                try {
                    if (perms.has(member, guild, Nodes.INFO_SERVER).get(5, TimeUnit.SECONDS)) {
                        reply(event, serverInfo(guild, bot));
                    }
                } catch (Exception e) {
                    replyError(event,"Failed to check permissions!");
                }
            }
            case "roles" -> {
                OptionMapping roleOpt = event.getOption("role");
                if (roleOpt == null) {
                    replyError(event, "Role is not defined!");
                    return;
                }
                Role role = roleOpt.getAsRole();

                try {
                    if (perms.has(member, guild, Nodes.INFO_ROLE).get(5, TimeUnit.SECONDS)) {
                        reply(event, roleInfo(guild, role, bot));
                        return;
                    }
                } catch (Exception e) {
                    replyError(event, "Failed to check permissions!");
                    return;
                }
                replyError(event, "You don't have permission to use this command!");
            }
            case "users" -> {
                OptionMapping roleOpt = event.getOption("user");
                if (roleOpt == null) {
                    replyError(event, "Role is not defined!");
                    return;
                }
                User user = roleOpt.getAsUser();

                try {
                    if (perms.has(member, guild, Nodes.INFO_USER).get(5, TimeUnit.SECONDS)) {
                        reply(event, userInfo(guild, user, bot));
                        return;
                    }
                } catch (Exception e) {
                    replyError(event, "Failed to check permissions!");
                    return;
                }
                replyError(event, "You don't have permission to use this command!");
            }
            default -> {
                replyError(event, "Invalid selection!");
            }
        }
    }

    private EmbedBuilder userInfo(Guild guild, User user, User bot) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
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

        eb.addField("\uD83D\uDCDB Username:", "*"+user.getName()+"*", true);
        eb.addField("\uD83C\uDFF7 Nickname:", "*"+nickname+"*", true);
        eb.addField("\uD83D\uDD16 GlobalName:", "*"+user.getGlobalName()+"*", true);

        eb.addField("⏰ Account Created:", "*"+user.getTimeCreated().format(formatter)+"*", true);
        eb.addField("⏰ Guild Join:", "*"+joinDate+"*", true);

        eb.addField("\uD83E\uDEAA User ID:", "*"+user.getId()+"*", true);
        eb.addField("\uD83E\uDD16 IsBot:", "*"+(user.isBot() ? "Yes" : "No")+"*", true);

        try {
            User.Profile profile = user.retrieveProfile().submit().get(5, TimeUnit.SECONDS);
            String banner = profile.getBanner() != null ? profile.getBanner().getUrl(512) : profile.getBannerUrl();

            eb.addField("\uD83D\uDDBC Profile Images:" , "*[Avatar]("+avatar+")*\n*[Banner]("+banner+")*",true);
            eb.setImage(banner);

            Color ac = profile.getAccentColor();
            String color = ac != null ? "(RGB: "+ac.getRed()+","+ac.getGreen()+","+ac.getBlue()+")" : "Unknown";

            eb.addField("\uD83D\uDD8D AccentColor:", "*" + color + "*",true);
        } catch (Exception e) {
            logger.error("Failed to retreive user profile for " + user.getGlobalName() + "("+user.getId()+")");
            eb.addField("\uD83D\uDDBC Profile Images:" , "*[Avatar]("+avatar+")*",true);
            eb.addField("\uD83D\uDD8D AccentColor:", "*Unknown*",true);
        }

        eb.addField("\uD83D\uDDD2 Permissions", "*"+Perms+"*", false);
        eb.addField("\uD83D\uDC6A Roles (" +(roleCount > 0 ? roleCount : "?" )+ "):", "*"+roles+"*", false);

        return eb;
    }
    private EmbedBuilder roleInfo(Guild guild, Role role, User bot) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        eb.setAuthor("Role Information", bot.getAvatarUrl());
        eb.setTitle(role.getName());

        int members = guild.getMembersWithRoles(role).size();

        String Perms = role.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", "));

        String icon = "*Unknown*";
        if (role.getIcon() != null) {
            icon = role.getIcon().getIcon() != null ? role.getIcon().getIcon().getUrl(512) : role.getIcon().getIconUrl();
            if (icon != null && !icon.isEmpty()) {
                eb.setThumbnail(icon);
                icon = "*[Icon]("+icon+")*";
            }
        }

        Color ac = role.getColor();
        String color = ac != null ? "(RGB: "+ac.getRed()+","+ac.getGreen()+","+ac.getBlue()+")" : "Unknown";

        eb.addField("\uD83E\uDEAA Role ID:", "*" + role.getId() + "*", true);
        eb.addField("\uD83D\uDCCB Total Members:", "*" + members + "*", true);
        eb.addField("\uD83D\uDD8D Color:", "*" + color + "*", true);
        if (icon != null){
            eb.addField("\uD83D\uDDBC Image:", icon, true);
        }

        eb.addField("⏰ Creation date:", role.getTimeCreated().format(formatter), false);
        eb.addField("\uD83D\uDDD2 Permissions:", Perms, false);

        return eb;
    }

    private EmbedBuilder serverInfo(Guild guild, User bot) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        eb.setAuthor("Server Information", bot.getAvatar() != null ? bot.getAvatar().getUrl(512) : bot.getAvatarUrl());
        eb.setTitle(guild.getName());
        eb.setThumbnail(guild.getIcon() != null ? guild.getIcon().getUrl(512) : guild.getIconUrl());

        if (guild.getDescription() != null) {
            eb.setDescription(
                    "\uD83D\uDD16 **Name:**\n*" + guild.getName() +"*"+
                            "\n\n" +
                            "\uD83D\uDCD6 **Description:**\n*" + guild.getDescription() + "*" +
                            "\n\n" +
                            "\uD83D\uDC82 **Explicit Level:**\n*" + guild.getExplicitContentLevel().getDescription() + "*" +
                            "\n\u200E"
            );
        }

        if (guild.getOwner() != null) {
            eb.addField("\uD83D\uDC51 Owner:", "*"+guild.getOwner().getUser().getGlobalName()+"*", true);
        }
        eb.addField("\uD83D\uDCC7 Server ID:", "*"+guild.getId()+"*", true);
        eb.addField("\uD83D\uDD1E NSWF Level:", "*"+formatNswfLevel(guild.getNSFWLevel())+"*", true);
        eb.addField("\uD83D\uDD25 Nitro boosters:", "*"+guild.getBoostCount()+"*", true);
        eb.addField("\uD83D\uDCA5 Boost Tier:", "*"+formatBoostTier(guild.getBoostTier())+"*", true);
        eb.addField("\uD83D\uDD52 Creation Date:", "*"+guild.getTimeCreated().format(formatter)+"*", true);

        eb.addField("⌛ AFK Timeout:", "*"+guild.getAfkTimeout().getSeconds()+"*",true);
        eb.addField("☎️ MFA Level:", "*"+formatMFA(guild.getRequiredMFALevel())+"*",true);

        eb.addField("\uD83D\uDD11 Verify Level:", "*"+formatVerificationLevel(guild.getVerificationLevel())+"*",true);

        eb.addField("\uD83D\uDCDD Total Members:", "*"+guild.getMemberCount()+"*", true);

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

            eb.addField("\uD83D\uDFE2 Online:", "*"+online+"*", false);
            eb.addField("⚫  Offline:", "*"+offline+"*", false);
            eb.addField("\uD83D\uDD34 Dnd:", "*"+disturb+"*", false);
            eb.addField("\uD83D\uDFE1 Idle:", "*"+idle+"*", false);
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
