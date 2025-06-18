package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

public class Prune extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();

    public Prune() {
        super("prune");
        this.setDescription("Prune x amount of chat history!");
        this.setDeleteDelay(5);
        this.setPermission(Nodes.PRUNE);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.INTEGER, "amount", "Amount of message to delete")
                        .setRequired(true)
                        .setRequiredRange(1,100),
                new OptionData(OptionType.STRING, "mode", "Cleaning mode")
                        .setRequired(true)
                        .addChoice("all","all")
                        .addChoice("bot","bot")
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        long amount = Objects.requireNonNull(event.getOption("amount")).getAsLong();
        String mode = Objects.requireNonNull(event.getOption("mode")).getAsString();

        MessageChannel channel = event.getMessageChannel();
        Member member = event.getMember();

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

        reply(event, eb);
    }

}
