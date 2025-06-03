package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.handlers.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SocialCredit extends BaseCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private CreditHandler credits = FoxBot.getCreditHandler();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public SocialCredit() {
        super("social");
        this.setDescription("Manage social credit system");
        this.setHidden(true);
        this.addArguments(
                new OptionData(OptionType.STRING, "action", "What you want to do?").setRequired(false)
                        .addChoice("give", "give")
                        .addChoice("take", "take")
                        .addChoice("set", "set")
                        .addChoice("top", "top")
                        .addChoice("get", "get"),
                new OptionData(OptionType.USER, "user", "Target user!").setRequired(false),
                new OptionData(OptionType.INTEGER, "amount", "Amount!").setRequired(false)
        );
    }

    @Override @SneakyThrows
    public Object onCommand(JDA jda, Member member, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = FoxFrame.embedTemplate();

        OptionMapping actionOPT = getOption(args, "action");
        if (actionOPT == null) {
            if (!perms.has(member, guild, Nodes.SOCIAL).get(10, TimeUnit.SECONDS)) {
                return error("You don't have permission to do that!");
            }

            credits.get(member.getUser(), balance -> {
                eb.setTitle("Your social credits");
                eb.setDescription("Total: " + balance);
                eb.setThumbnail(member.getUser().getAvatarUrl());
            });
            return eb;
        }
        String action = actionOPT.getAsString();

        eb.setTitle("Social credits");

        if (action.equalsIgnoreCase("top")) {
            if (!perms.has(member, guild, Nodes.SOCIAL_TOP).get(10, TimeUnit.SECONDS)) {
                return error("You don't have permission to do that!");
            }

            eb.setTitle("Top 10 Social credits");
            credits.top10(member.getUser(), top -> {
                List<String> entries = new ArrayList<>();
                int i = 1;
                User top1 = null;
                for (UserEntry entry : top) {
                    User target = guild.getJDA().getUserById(entry.getUserID());
                    if (target != null && target.isBot()) {continue;}

                    if (target == null) continue;
                    String name = target.getGlobalName();

                    if (top1 == null) {
                        top1 = target;
                        entries.add("**" + i + ". " + name + " - " + entry.getSocialCredits() + "**");
                    } else {
                        entries.add(i + ". " + name + " - " + entry.getSocialCredits());
                    }

                    i++;
                }
                eb.setThumbnail(top1 == null ? null : top1.getAvatarUrl());
                eb.setDescription(String.join("\n", entries));
            });
            return eb;
        }

        if (!perms.has(member, guild, Nodes.SOCIAL_ADMIN).get(10, TimeUnit.SECONDS)) {
            return error("You don't have permission to do that!");
        }

        OptionMapping userOPT = getOption(args, "user");
        if (userOPT == null) {
            return error("Invalid user!");
        }
        User target = userOPT.getAsUser();
        eb.setThumbnail(target.getAvatarUrl());

        if (target.isBot()) {
            return error("You can't alter bots social credits!");
        }

        switch (action) {
            case "give" -> {
                OptionMapping opt = getOption(args, "amount");
                if (opt == null) {
                    logger.error(member.getUser().getGlobalName() + " tried to give social credits but failed because amount was not defined!");
                    return error("Invalid amount!");
                }

                credits.add(target, opt.getAsInt());
                logger.info(member.getUser().getGlobalName() + " gave " + target.getGlobalName() + " " + opt.getAsInt() + " social credits!");

                eb.setDescription("Gave " + target.getGlobalName() + " " + opt.getAsInt() + " social credits!");
                credits.get(target, balance -> {
                    eb.setDescription("Gave " + target.getGlobalName() + " " + opt.getAsInt() + " social credits!" + "\n\n" + "Credits: " + balance);
                });

            }
            case "take" -> {
                OptionMapping opt = getOption(args, "amount");
                if (opt == null) {
                    logger.error(member.getUser().getGlobalName() + " tried to take social credits but failed because amount was not defined!");
                    return error("Invalid amount!");
                }

                credits.take(target, opt.getAsInt());
                logger.info(member.getUser().getGlobalName() + " took " + opt.getAsInt() + " social credits from " + target.getGlobalName() + "!");

                eb.setDescription("Took " + opt.getAsInt() + " social credits from " + target.getGlobalName() + "!");
                credits.get(target, balance -> {
                    eb.setDescription("Took " + opt.getAsInt() + " social credits from " + target.getGlobalName() + "!" + "\n\n" + "Credits: " + balance);
                });

            }
            case "set" -> {
                OptionMapping opt = getOption(args, "amount");
                if (opt == null) {
                    logger.error(member.getUser().getGlobalName() + " tried to set social credits but failed because amount was not defined!");
                    return error("Invalid amount!");
                }

                credits.set(target, opt.getAsInt());
                logger.info(member.getUser().getGlobalName() + " set " + target.getGlobalName() + "'s social credits to " + opt.getAsInt() + "!");

                eb.setDescription("Set " + target.getGlobalName() + "'s social credits to " + opt.getAsInt() + "!");

            }
            case "get" -> {
                credits.get(target, amount -> {
                    eb.setTitle(target.getGlobalName() + "'s social credits");
                    eb.setDescription("Social credits: " + amount);
                    eb.setThumbnail(target.getAvatarUrl());
                });
            }
        }

        return eb;
    }


}
