package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Permissions;
import fi.natroutter.foxbot.objects.*;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxlib.Handlers.NATLogger;
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

    private NATLogger logger = FoxBot.getLogger();

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
    public void onModalInteraction(ModalInteractionEvent e) {
        for (BaseCommand cmd : commands) {
            for (BaseModal bm : cmd.getModals()) {
                if (e.getModalId().equalsIgnoreCase(bm.getId())) {

                    Object reply = cmd.onModalSubmit(e.getMember(), e.getJDA().getSelfUser(), e.getGuild(), e.getMessageChannel(), bm, e.getValues());

                    if (reply == null) {
                        e.reply("Error : Invalid modal reply!").queue();
                        return;
                    }

                    boolean hidden = cmd.isCommandReplyTypeModal();
                    if (reply instanceof BaseReply br) {
                        reply = br.getObject();
                        hidden = br.isHidden();
                    }

                    if (reply instanceof EmbedBuilder) {
                        e.replyEmbeds(((EmbedBuilder) reply).build()).setEphemeral(hidden).queue();
                    } else if (reply instanceof String str) {
                        e.reply(str).setEphemeral(hidden).queue();
                    } else {
                        e.reply("Error : Invalid modal reply submitted!").setEphemeral(hidden).queue();
                    }

                    return;

                }
            }
        }
        e.reply("Error : cant find modal!").queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
        if (e.getGuild() == null) return;
        if (e.getMember() == null) return;

        for (BaseCommand cmd : commands) {
            if (e.getName().equalsIgnoreCase(cmd.getName())) {

                if (cmd.isOnCooldown(e.getMember())) {
                    e.reply("You are no cooldown for next " + cmd.getCooldown(e.getMember()) + " seconds").setEphemeral(true).queue();
                    return;
                }
                cmd.setCooldown(e.getMember());

                if (cmd.isCommandReplyTypeModal()) {
                    Object reply = cmd.onCommand(e.getMember(), bot.getJda().getSelfUser(), e.getGuild(), e.getMessageChannel(), e.getOptions());

                    if (reply == null) {
                        e.reply("Error : Invalid command modal reply!").queue();
                        return;
                    }

                    if (reply instanceof ModalReply mr) {
                        Modal modal = Modal.create(cmd.getName(), mr.getModalName()).addActionRows(mr.getRows()).build();
                        e.replyModal(modal).queue();
                    } else {
                        e.reply("Command error in "+cmd.getName()+" : reply type needs to be \"ModalReply\"").queue();
                    }
                    return;
                }

                logger.info(e.getUser().getAsTag() + "("+e.getUser().getId()+") Used command \"" + cmd.getName() + "\" on channel \"" + e.getChannel().getName() + "("+e.getChannel().getId()+")\" in guild \"" + e.getGuild().getName() + "("+e.getGuild().getId()+")\"");

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
                        user = e.getMember().getUser().getAsTag();
                    }

                    logger.info(user +" pressed button "+btn.getId()+" on channel #"+e.getMessageChannel().getName()+"("+e.getMessageChannel().getId()+")");
                    Object reply = cmd.onButtonPress(e.getMember(), e.getJDA().getSelfUser(), e.getGuild(), e.getMessageChannel(), btn);

                    if (reply == null) {
                        e.reply("Error : Invalid button action!").queue();
                        return;
                    }

                    boolean hidden = true;
                    if (reply instanceof BaseReply br) {
                        reply = br.getObject();
                        hidden = br.isHidden();
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

                    e.reply("Error : Invalid button reply!").queue();
                    return;
                }
            }
        }
        e.reply("Error : cant find button action!").queue();
    }

    public void commandReply(SlashCommandInteractionEvent e, BaseCommand cmd) {
        Object reply = cmd.onCommand(e.getMember(), bot.getJda().getSelfUser(), e.getGuild(), e.getMessageChannel(), e.getOptions());
        if (reply == null) {
            e.reply("Error : Invalid command reply!").queue();
            return;
        }

        if (reply instanceof EmbedBuilder eb) {
            e.getHook().editOriginalEmbeds(eb.build()).queue();
        } else if (reply instanceof Modal) {
            e.getHook().editOriginal("Command error in "+cmd.getName()+" : You cant reply modal here!").queue();
        } else {
            String msg = reply.toString();
            e.getHook().editOriginal(msg).queue();
        }

        if (cmd.getDeleteDelay() > 0) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    e.getHook().deleteOriginal().queue();
                }
            }, cmd.getDeleteDelay() * 1000L);
        }
    }

}
