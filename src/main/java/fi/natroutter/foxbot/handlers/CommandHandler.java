package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Permission;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.handlers.permissions.Permissions;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.objects.*;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.*;

public class CommandHandler extends ListenerAdapter {

    private BotHandler bot;

    @Getter
    private List<BaseCommand> commands = new ArrayList<>();

    private FoxLogger logger = FoxBot.getLogger();

    public CommandHandler(BotHandler bot) {
        this.bot = bot;
    }

    protected void registerAll() {
        List<CommandData> cmds = new ArrayList<>();
        for (BaseCommand cmd : commands) {

            cmd.setOnCooldownRemoved(data -> {
                logger.warn("Removed old cooldown from user \"" + data.userID() + "\" with \""+data.command().getCooldownSeconds()+" seconds\" in command \"" + data.command().getName() + "\"");
            });

            logger.info("Registering command : " + cmd.getName());

            SlashCommandData data = Commands.slash(cmd.getName().toLowerCase(), cmd.getDescription());

            if (cmd.getArguments().size() > 0) {
                data.addOptions(cmd.getArguments());
            }
            cmds.add(data);
        }
        for (Guild guild : bot.getJda().getGuilds()) {
            guild.updateCommands().addCommands(cmds).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent e) {
        for (BaseCommand cmd : commands) {
            for (BaseStringMenu menu : cmd.getStringMenus()) {
                if (e.getComponentId().equals(menu.getMenu().getId())) {

                    Object reply = cmd.onStringMenuSelect(e.getMember(), e.getJDA().getSelfUser(), e.getGuild(), e.getMessageChannel(), menu, e.getSelectedOptions());

                    if (reply == null) {
                        e.replyEmbeds(Utils.errorEmbed("Error : Invalid menu reply").build()).queue();
                        logger.error("Invalid StringMenu response occurred in " + cmd.getName()+ " at menu " + menu.getId() + " code: x0001");
                        return;
                    }

                    boolean hidden = cmd.isCommandReplyTypeModal();
                    if (reply instanceof BaseReply br) {
                        reply = br.getObject();
                        hidden = br.isHidden();
                        if (br.getDeleteDelay() > 0) {
                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    try {
                                        if (!e.getHook().isExpired()) {
                                            e.getHook().deleteOriginal().complete();
                                        }
                                    } catch (Exception ignore) {}
                                }
                            }, br.getDeleteDelay() * 1000L);
                        }
                    }

                    if (reply instanceof EmbedBuilder) {
                        e.replyEmbeds(((EmbedBuilder) reply).build()).setEphemeral(hidden).queue();
                    } else if (reply instanceof String str) {
                        e.reply(str).setEphemeral(hidden).queue();
                    } else {
                        e.replyEmbeds(Utils.errorEmbed("Error : menu doesn't exists!").build()).setEphemeral(hidden).queue();
                        logger.error("modal not found in " + cmd.getName()+ " at menu " + menu.getId());
                    }
                    return;

                }
            }
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent e) {
        for (BaseCommand cmd : commands) {
            for (BaseModal bm : cmd.getModals()) {
                if (e.getModalId().equalsIgnoreCase(bm.getId())) {

                    Object reply = cmd.onModalSubmit(e.getMember(), e.getJDA().getSelfUser(), e.getGuild(), e.getMessageChannel(), bm, e.getValues());

                    if (reply == null) {
                        e.replyEmbeds(Utils.errorEmbed("Error : Invalid modal reply").build()).queue();
                        logger.error("Invalid modal response occurred in " + cmd.getName()+ " at modal " + bm.getId() + " code: x0001");
                        return;
                    }

                    boolean hidden = cmd.isCommandReplyTypeModal();
                    if (reply instanceof BaseReply br) {
                        reply = br.getObject();
                        hidden = br.isHidden();
                        if (br.getDeleteDelay() > 0) {
                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    try {
                                        if (!e.getHook().isExpired()) {
                                            e.getHook().deleteOriginal().complete();
                                        }
                                    } catch (Exception ignore) {}
                                }
                            }, br.getDeleteDelay() * 1000L);
                        }
                    }

                    if (reply instanceof EmbedBuilder) {
                        e.replyEmbeds(((EmbedBuilder) reply).build()).setEphemeral(hidden).queue();
                    } else if (reply instanceof String str) {
                        e.reply(str).setEphemeral(hidden).queue();
                    } else {
                        e.replyEmbeds(Utils.errorEmbed("Error : modal doesn't exists!").build()).setEphemeral(hidden).queue();
                        logger.error("modal not found in " + cmd.getName()+ " at modal " + bm.getId());
                    }
                    return;
                }
            }
        }
        e.replyEmbeds(Utils.errorEmbed("Error : modal doesn't exists!").build()).queue();
        logger.error("Fatal error occured modal not found!");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) return;
        if (e.getMember() == null) return;

        for (BaseCommand cmd : commands) {
            if (e.getName().equalsIgnoreCase(cmd.getName())) {

                if (cmd.isOnCooldown(e.getMember())) {
                    e.replyEmbeds(Utils.errorEmbed("You are on a command cooldown for " + cmd.getCooldown(e.getMember()) + " seconds").build()).setEphemeral(true).queue();
                    logger.warn("User " + e.getMember().getUser().getGlobalName() + " used command " + cmd.getName() + " but is on cooldown!");
                    return;
                }
                if (cmd.getCooldownSeconds() > 0) {
                    Permissions.has(e.getMember(), Node.BYPASS_COOLDOWN, (has)->{
                        if (!has) {
                            cmd.setCooldown(e.getMember());
                        }
                    });
                }

                if (cmd.isCommandReplyTypeModal()) {
                    Object reply = cmd.onCommand(e.getMember(), bot.getJda().getSelfUser(), e.getGuild(), e.getMessageChannel(), e.getOptions());

                    if (reply == null) {
                        e.replyEmbeds(Utils.errorEmbed("Error : Invalid modal reply").build()).queue();
                        logger.error("Invalid modal response occurred in " + cmd.getName() + " because it's null");

                        return;
                    }

                    if (reply instanceof ModalReply mr) {
                        Modal modal = Modal.create(cmd.getName(), mr.getModalName()).addActionRows(mr.getRows()).build();
                        e.replyModal(modal).queue();
                    } else {
                        e.replyEmbeds(Utils.errorEmbed("Error in command "+cmd.getName()+" : Response type needs to be \"ModalReply\"").build()).queue();
                        logger.error("Invalid command reply type needs to be \"ModalReply\" in command " + cmd.getName());
                    }
                    return;
                }

                logger.info(e.getUser().getGlobalName() + "("+e.getUser().getId()+") Used command \"" + cmd.getName() + " " + commandArgs(e.getOptions()) + "\" on channel \"" + e.getChannel().getName() + "("+e.getChannel().getId()+")\" in guild \"" + e.getGuild().getName() + "("+e.getGuild().getId()+")\"");

                if (cmd.getPermission() == null) {
                    e.deferReply(cmd.isHidden()).queue();
                    commandReply(e, cmd);
                    return;
                }

                Permissions.has(e.getMember(), cmd.getPermission(), has->{
                    if (has) {
                        e.deferReply(cmd.isHidden()).queue();
                        commandReply(e, cmd);
                    } else {
                        e.reply("You don't have permissions to use this command!").setEphemeral(true).queue();
                        logger.warn(e.getUser().getGlobalName() + " tried to use command " + cmd.getName() + " but doesn't have permissions!");
                    }
                });
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent e) {
        for (BaseCommand cmd : commands) {
            for (BaseButton btn : cmd.getButtons()) {
                if (e.getComponentId().equalsIgnoreCase(btn.getButton().getId())) {

                    String user = "Unknown";
                    if (e.getMember() != null) {
                        user = e.getMember().getUser().getGlobalName();
                    }

                    logger.info(user +" pressed button "+btn.getId()+" on channel #"+e.getMessageChannel().getName()+"("+e.getMessageChannel().getId()+")");
                    Object reply = cmd.onButtonPress(e.getMember(), e.getJDA().getSelfUser(), e.getGuild(), e.getMessageChannel(), btn);

                    if (reply == null) {
                        e.replyEmbeds(Utils.errorEmbed("Error : Invalid button action!").build()).queue();
                        logger.error("Invalid button action at " + btn.getId() + " in command "  + cmd.getName());
                        return;
                    }

                    boolean hidden = true;
                    if (reply instanceof BaseReply br) {
                        reply = br.getObject();
                        hidden = br.isHidden();
                        if (br.getDeleteDelay() > 0) {
                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    try {
                                        if (!e.getHook().isExpired()) {
                                            e.getHook().deleteOriginal().complete();
                                        }
                                    } catch (Exception ignore) {}
                                }
                            }, br.getDeleteDelay() * 1000L);
                        }
                    }

                    if (reply instanceof EmbedBuilder eb) {
                        e.replyEmbeds(eb.build()).setEphemeral(hidden).queue();
                        return;
                    } else if (reply instanceof BaseModal mr) {
                        Modal modal = Modal.create(mr.getId(), mr.getModal().getModalName()).addActionRows(mr.getModal().getRows()).build();
                        e.replyModal(modal).queue();
                        return;
                    } else if (reply instanceof String str) {
                        e.reply(reply.toString()).setEphemeral(hidden).queue();
                        return;
                    }

                    e.replyEmbeds(Utils.errorEmbed("Error : Invalid button reply!").build()).queue();
                    logger.error("Invalid button reply at " + btn.getId() + " in command "  + cmd.getName());
                    return;
                }
            }
        }
        e.replyEmbeds(Utils.errorEmbed("Error : Button action doesn't exists!").build()).queue();
        logger.error("Fatal error occured: Button not found!");
    }

    public String commandArgs(List<OptionMapping> args) {
        StringBuilder str = new StringBuilder();
        for (OptionMapping arg : args) {
            switch (arg.getType()) {
                case UNKNOWN, NUMBER, STRING, SUB_COMMAND, SUB_COMMAND_GROUP -> str.append(arg.getName()).append(":").append(arg.getAsString()).append(" ");
                case INTEGER -> str.append(arg.getName()).append(":").append(arg.getAsInt()).append(" ");
                case BOOLEAN -> str.append(arg.getName()).append(":").append(arg.getAsBoolean()).append(" ");
                case USER -> str.append(arg.getName()).append(":").append(arg.getAsUser().getGlobalName()).append(" ");
                case CHANNEL -> str.append(arg.getName()).append(":").append(arg.getAsChannel().getId()).append(" ");
                case ROLE -> str.append(arg.getName()).append(":").append(arg.getAsRole().getId()).append(" ");
                case MENTIONABLE -> str.append(arg.getName()).append(":").append(arg.getAsMentionable().getId()).append(" ");
                case ATTACHMENT -> str.append(arg.getName()).append(":").append(arg.getAsAttachment().getUrl()).append(" ");
            }
        }
        return str.toString();
    }

    public void commandReply(SlashCommandInteractionEvent e, BaseCommand cmd) {
        Object reply = cmd.onCommand(e.getMember(), bot.getJda().getSelfUser(), e.getGuild(), e.getMessageChannel(), e.getOptions());
        if (reply == null) {
            e.replyEmbeds(Utils.errorEmbed("Error : Invalid command reply!").build()).queue();
            logger.error("Invalid command reply at " + cmd.getName() + " reply is null!");
            return;
        }

        if (reply instanceof EmbedBuilder eb) {
            e.getHook().sendMessageEmbeds(eb.build()).queue();
        } else if (reply instanceof Modal) {
            e.getHook().editOriginal("Error in command "+cmd.getName()+" : Cant reply modal here!").queue();
            logger.error("Can't reply modal in " + cmd.getName());
        } else {
            String msg = reply.toString();
            e.getHook().editOriginal(msg).queue();
        }

        if (cmd.getDeleteDelay() > 0) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    try {
                        if (!e.getHook().isExpired()) {
                            e.getHook().deleteOriginal().complete();
                        }
                    } catch (Exception ignore) {}
                }
            }, cmd.getDeleteDelay() * 1000L);
        }
    }

}
