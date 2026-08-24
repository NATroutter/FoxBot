package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.feature.socialcredit.SocialCreditHandler;
import fi.natroutter.foxbot.feature.socialcredit.listeners.SocialCreditButtonListener;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SocialCredit extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private SocialCreditHandler credits = FoxBot.getSocialCreditHandler();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public SocialCredit() {
        super("social");
        this.setDescription("Manage social credit system");
    }

    /**
     * The leaderboard as a rendered card stack, the same look the voice session leaderboard uses.
     *
     * <p>Deferred and drawn off the JDA thread: the first render of a face fetches it from
     * Discord's CDN, which is not something an event thread should be waiting on. Paging is handled
     * by {@link SocialCreditButtonListener}, which redraws through the same method.
     */
    private void showTop(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> SocialCreditButtonListener.openBoard(hook, event.getJDA()));
    }

    public User getTarget(SlashCommandInteractionEvent event) {
        OptionMapping userOPT = event.getOption("user");
        if (userOPT == null) {
            replyError(event, "Invalid user!");
            return null;
        }
        User target = userOPT.getAsUser();

        if (target.isBot()) {
            replyError(event,"You can't alter bots social credits!");
            return null;
        }
        return target;
    }
    public Integer getAmount(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        OptionMapping opt = event.getOption("amount");
        if (opt == null) {
            logger.error(member.getUser().getGlobalName() + " tried to give social credits but failed because amount was not defined!");
            replyError(event, "Invalid amount!");
            return null;
        }
        return opt.getAsInt();
    }

    @Override
    public List<OptionData> options() {
        return List.of(
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
    public void onCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = FoxFrame.embedTemplate();

        Member member = event.getMember();
        Guild guild = event.getGuild();

        OptionMapping actionOPT = event.getOption("action");
        if (actionOPT == null) {
            if (!perms.has(member, guild, Nodes.SOCIAL).get(10, TimeUnit.SECONDS)) {
                replyError(event, "You don't have permission to do that!");
                return;
            }

            credits.get(member.getUser(), balance -> {
                eb.setTitle("Your social credits");
                eb.setDescription("Total: " + balance);
                eb.setThumbnail(member.getUser().getAvatarUrl());
            });
            reply(event, eb);
            return;
        }

        String action = actionOPT.getAsString();
        eb.setTitle("Social credits");

        final String globalName = member.getUser().getGlobalName();
        switch (action) {

            case "top" -> {
                if (!perms.has(member, guild, Nodes.SOCIAL_TOP).get(10, TimeUnit.SECONDS)) {
                    replyError(event, "You don't have permission to do that!");
                    return;
                }
                showTop(event);
                return;
            }

            case "give" -> {
                if (!perms.has(member, guild, Nodes.SOCIAL_ADMIN).get(10, TimeUnit.SECONDS)) {
                    replyError(event, "You don't have permission to do that!");
                    return;
                }
                User target = getTarget(event);
                if (target == null) return;
                eb.setThumbnail(target.getAvatarUrl());

                Integer amount = getAmount(event);
                if (amount == null) return;

                credits.add(target, amount);
                logger.info(globalName + " gave " + target.getGlobalName() + " " + amount + " social credits!");

                eb.setDescription("Gave " + target.getGlobalName() + " " + amount + " social credits!");
                credits.get(target, balance -> {
                    eb.setDescription("Gave " + target.getGlobalName() + " " + amount + " social credits!" + "\n\n" + "Credits: " + balance);
                });

            }

            case "take" -> {
                if (!perms.has(member, guild, Nodes.SOCIAL_ADMIN).get(10, TimeUnit.SECONDS)) {
                    replyError(event, "You don't have permission to do that!");
                    return;
                }
                User target = getTarget(event);
                if (target == null) return;
                eb.setThumbnail(target.getAvatarUrl());

                Integer amount = getAmount(event);
                if (amount == null) return;

                credits.take(target, amount);
                logger.info(globalName + " took " + amount + " social credits from " + target.getGlobalName() + "!");

                eb.setDescription("Took " + amount + " social credits from " + target.getGlobalName() + "!");
                credits.get(target, balance -> {
                    eb.setDescription("Took " + amount + " social credits from " + target.getGlobalName() + "!" + "\n\n" + "Credits: " + balance);
                });

            }

            case "set" -> {
                if (!perms.has(member, guild, Nodes.SOCIAL_ADMIN).get(10, TimeUnit.SECONDS)) {
                    replyError(event, "You don't have permission to do that!");
                    return;
                }
                User target = getTarget(event);
                if (target == null) return;
                eb.setThumbnail(target.getAvatarUrl());

                Integer amount = getAmount(event);
                if (amount == null) return;

                credits.set(target, amount);
                logger.info(globalName + " set " + target.getGlobalName() + "'s social credits to " + amount + "!");

                eb.setDescription("Set " + target.getGlobalName() + "'s social credits to " + amount + "!");

            }

            case "get" -> {
                if (!perms.has(member, guild, Nodes.SOCIAL_ADMIN).get(10, TimeUnit.SECONDS)) {
                    replyError(event, "You don't have permission to do that!");
                    return;
                }
                User target = getTarget(event);
                if (target == null) return;
                eb.setThumbnail(target.getAvatarUrl());

                credits.get(target, amount -> {
                    eb.setTitle(target.getGlobalName() + "'s social credits");
                    eb.setDescription("Social credits: " + amount);
                    eb.setThumbnail(target.getAvatarUrl());
                });
            }
        }
        reply(event, eb);
    }


}
