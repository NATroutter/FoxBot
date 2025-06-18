package fi.natroutter.foxbot.feature.socialcredit.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.feature.socialcredit.SocialCreditHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class SocialMessageReceiveListener extends ListenerAdapter {

    private SocialCreditHandler credit = FoxBot.getSocialCreditHandler();
    private FoxLogger logger = FoxBot.getLogger();
    private Config config = FoxBot.getConfig().get();

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        User author = e.getAuthor();
        Message msg = e.getMessage();
        MessageChannelUnion chan = e.getChannel();

        if (author.isBot()) return;

        String foodStashID = config.getChannels().getFoodStash();

        //This is ugly I know it, but there are reason for this mess!
        if (e.getChannelType().equals(ChannelType.GUILD_PUBLIC_THREAD) && !(
                e.getMessage().getContentRaw().length() > 0 ||
                        e.getMessage().getAttachments().size() > 0 ||
                        e.getMessage().getStickers().size() > 0 ||
                        e.getMessage().getEmbeds().size() > 0
        )) return;

        if (e.getMessage().getType().equals(MessageType.THREAD_CREATED)) return;

        //if channel is foodStash
        if (chan.getId().equalsIgnoreCase(foodStashID) && e.getChannelType().equals(ChannelType.TEXT)) {
            EmbedBuilder em = FoxFrame.error("Attention!", "**Hey** "+author.getAsMention()+"\nOnly food related images are allowed in this channel\n\nIf you want to comment on someones image/post you need to start a thread!");

            if (msg.getAttachments().isEmpty()) {
                e.getMessage().delete().queue();
                e.getChannel().sendMessageEmbeds(em.build()).queue(m->{
                    m.delete().queueAfter(20, TimeUnit.SECONDS);
                });
                //Utils.sendPrivateMessage(author, em, "only_images_foodStash");
                return;
            }

            boolean hasOther = false;
            boolean hasImage = false;
            for(Message.Attachment attachment : msg.getAttachments()) {
                if (attachment.isImage()) {
                    hasImage = true;
                } else {
                    hasOther = true;
                }
            }
            if (hasOther || !hasImage) {
                e.getMessage().delete().queue();
                e.getChannel().sendMessageEmbeds(em.build()).queue(m->{
                    m.delete().queueAfter(20, TimeUnit.SECONDS);
                });
                //Utils.sendPrivateMessage(author, em, "only_images_foodStash");
                return;
            }

            Config.Emojies emojies = config.getEmojies();
            msg.addReaction(emojies.getUpvote().asEmoji()).queue();
            msg.addReaction(emojies.getDownvote().asEmoji()).queue();

            //Open a new thred
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            String threadName = msg.getAuthor().getName() + " - " + LocalDateTime.now().format(formatter);
            msg.createThreadChannel(threadName).queue();
            logger.info("New food thread created for new post by "+msg.getAuthor().getName()+" ("+threadName+")");

            if (SocialCreditHandler.useSocialCredits(config,chan)) {
                credit.add(e.getAuthor(), 10);
                logger.info(e.getAuthor().getGlobalName() + " sent an image to foodStash, giving 10 social credits");
            }

            return;
        }

        if (SocialCreditHandler.useSocialCredits(config,chan)) {
            //if channel is not foodStash!
            if (!msg.getAttachments().isEmpty()) {
                credit.add(e.getAuthor(), 2);
                logger.info(e.getAuthor().getGlobalName() + " sent an message with attachment, giving 2 social credits");
            } else {
                credit.add(e.getAuthor(), 1);
                logger.info(e.getAuthor().getGlobalName() + " sent an message, giving 1 social credit");
            }
        }

    }

}
