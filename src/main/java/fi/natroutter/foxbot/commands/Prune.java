package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class Prune extends BaseCommand {

    private FoxLogger logger = FoxBot.getLogger();

    public Prune() {
        super("prune");
        this.setDescription("prune x amount of chat history!");
        this.setDeleteDelay(5);
        this.setPermission(Nodes.PRUNE);
        this.addArguments(
                new OptionData(OptionType.INTEGER, "amount", "This is amount of message you want to delete")
                        .setRequired(true)
                        .setRequiredRange(1,100),
                new OptionData(OptionType.STRING, "mode", "Cleaning mode")
                        .setRequired(true)
                        .addChoice("all","all")
                        .addChoice("bot","bot")
        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        long amount = getOption(args, "amount").getAsLong();
        String mode = getOption(args, "mode").getAsString();

        List<Message> messages = channel.getHistory().retrievePast((int)amount).complete();
        for(Message message : messages) {
            if (message.isEphemeral() || message.isPinned()) {
                continue;
            }
            if (mode.equalsIgnoreCase("bot")) {
                if (!message.getAuthor().isBot()) {continue;}
            }
            message.delete().reason("Pruning chat").queue();

            if (message.getMember() != null){
                User user = message.getMember().getUser();
                logger.warn("Deleting message send by \""+ user.getName()+"("+ user.getId()+")\" from channel \"" + channel.getName() + "("+channel.getId()+")\"");
            }

        }

        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Chat Pruned!");
        eb.setDescription("Chat has been cleaned!");
        eb.addField("Requested by", member.getAsMention(), true);
        eb.addField("Amount", String.valueOf(amount), true);
        eb.addField("Channel", channel.getName(), true);

        return eb;
    }

}
