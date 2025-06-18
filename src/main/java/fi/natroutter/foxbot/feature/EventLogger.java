package fi.natroutter.foxbot.feature;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Wakeup;
import fi.natroutter.foxbot.data.MessageLog;
import fi.natroutter.foxlib.expiringmap.ExpirationPolicy;
import fi.natroutter.foxlib.expiringmap.ExpiringMap;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceVideoEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class EventLogger extends ListenerAdapter {

    private FoxLogger logger = FoxBot.getLogger();

    private ExpiringMap<Long, MessageLog> messageLog = ExpiringMap.builder()
            .expiration(60, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();

    public EventLogger() {}

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
        if(e.getMember().getUser().isBot() || e.getMember().getUser().isSystem()) return;

        if (e.getChannelJoined() != null && e.getChannelLeft() != null) {
            if(!e.getChannelJoined().getId().equals(e.getChannelLeft().getId())) {
                if (Wakeup.users.contains(e.getEntity().getIdLong())) return;
                logger.info(e.getEntity().getUser().getGlobalName() + " has been moved from (" + e.getChannelLeft().getName() + ") to (" + e.getChannelJoined().getName()+")");
                return;
            }
        }
        if (e.getChannelJoined() == null) {
            String name = e.getChannelLeft() == null ? "" : e.getChannelLeft().getName();
            logger.info(e.getMember().getUser().getGlobalName() + " left voice channel (" + name+")");
            return;
        }
        if (e.getChannelLeft() == null) {
            String name = e.getChannelJoined() == null ? "" : e.getChannelJoined().getName();
            logger.info(e.getMember().getUser().getGlobalName() + " joined voice channel (" + name+")");
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent e) {
        if(e.getUser().isBot() || e.getUser().isSystem()) return;

        logger.info(e.getUser().getGlobalName() + " joined the guild ("+e.getGuild().getName()+")");
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent e) {
        if(e.getUser().isBot() || e.getUser().isSystem()) return;

        logger.info(e.getUser().getGlobalName() + " has left the guild ("+e.getGuild().getName()+")");
    }

    @Override
    public void onGuildBan(GuildBanEvent e) {
        if(e.getUser().isBot() || e.getUser().isSystem()) return;

        logger.info(e.getUser().getGlobalName() + " has been banned from ("+e.getGuild().getName()+")");
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent e) {
        if(e.getUser().isBot() || e.getUser().isSystem()) return;

        logger.info(e.getUser().getGlobalName() + " has been unbanned from ("+e.getGuild().getName()+")");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if(e.getAuthor().isBot() || e.getAuthor().isSystem()) return;
        if (!e.isFromGuild()) return;

        if (e.getChannelType().equals(ChannelType.GUILD_PUBLIC_THREAD) && !(
                e.getMessage().getContentRaw().length() > 0 ||
                        e.getMessage().getAttachments().size() > 0 ||
                        e.getMessage().getStickers().size() > 0 ||
                        e.getMessage().getEmbeds().size() > 0
        )) return;

        if (e.getChannelType().equals(ChannelType.TEXT)) {
            MessageChannelUnion channel = e.getChannel();  //e.getChannel().asTextChannel();
            String attachments = e.getMessage().getAttachments().stream().map(a-> "  - " + a.getFileName() + " : " + a.getUrl()).collect(Collectors.joining("\n"));
            String stickers = e.getMessage().getStickers().stream().map(a-> "  - " + a.getName() + " : " + a.getIconUrl()).collect(Collectors.joining("\n"));
            logger.log("("+channel.getName()+") " + e.getAuthor().getGlobalName() + ": " + e.getMessage().getContentStripped()
                + ((attachments.length() > 0) ? "\nAttachments:\n" + attachments : "")
                + ((stickers.length() > 0) ? "\nStickers:\n" + stickers : "")
            );
        } else if (e.getChannelType().equals(ChannelType.GUILD_PUBLIC_THREAD)) {
            ThreadChannel thread = e.getChannel().asThreadChannel();
            String attachments = e.getMessage().getAttachments().stream().map(a-> "  - " + a.getFileName() + " : " + a.getUrl()).collect(Collectors.joining("\n"));
            String stickers = e.getMessage().getStickers().stream().map(a-> "  - " + a.getName() + " : " + a.getIconUrl()).collect(Collectors.joining("\n"));
            logger.log("("+ thread.getParentChannel().getName() +"/"+ thread.getName()+") " + e.getAuthor().getGlobalName() + ": " + e.getMessage().getContentStripped()
                    + ((attachments.length() > 0) ? "\nAttachments:\n" + attachments : "")
                    + ((stickers.length() > 0) ? "\nStickers:\n" + stickers : "")
            );
        }
        messageLog.put(e.getMessageIdLong(), new MessageLog(e.getMessage(), null, e.getGuild(), e.getChannel()));
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent e) {
        if(e.getAuthor().isBot() || e.getAuthor().isSystem()) return;

        if (messageLog.containsKey(e.getMessageIdLong())) {
            MessageLog old = messageLog.get(e.getMessageIdLong());

            if (e.getMessage().getContentStripped().isEmpty() && old.getMessage().getContentStripped().isEmpty()) {
                return;
            }

            old.setEdited(e.getMessage());

            String oldAttachments = old.getMessage().getAttachments().stream().map(a-> "  - " + a.getFileName() + " : " + a.getUrl()).collect(Collectors.joining("\n"));
            String newAttachments = e.getMessage().getAttachments().stream().map(a-> "  - " + a.getFileName() + " : " + a.getUrl()).collect(Collectors.joining("\n"));

            String oldstickers = old.getMessage().getStickers().stream().map(a-> "  - " + a.getName() + " : " + a.getIconUrl()).collect(Collectors.joining("\n"));
            String newstickers = e.getMessage().getStickers().stream().map(a-> "  - " + a.getName() + " : " + a.getIconUrl()).collect(Collectors.joining("\n"));

            logger.info(old.getMessage().getAuthor().getGlobalName()+"'s message edited! ["+old.getMessage().getId()+"](Old: "+old.getMessage().getContentStripped()+") (New: "+e.getMessage().getContentStripped()+")" +
                    ((newAttachments.length() > 0) ? "\nNew Attachments:\n" + newAttachments : "") +
                    ((oldAttachments.length() > 0) ? "\nOld Attachments:\n" + oldAttachments : "") +
                    ((newstickers.length() > 0) ? "\nNew Stickers:\n" + newstickers : "") +
                    ((oldstickers.length() > 0) ? "\nOld Stickers:\n" + oldstickers : "")
            );
            old.setMessage(e.getMessage());
            messageLog.put(e.getMessageIdLong(), old);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent e) {
        MessageChannelUnion channel = e.getChannel();

        if (messageLog.containsKey(e.getMessageIdLong())) {
            MessageLog deleted = messageLog.get(e.getMessageIdLong());
            Message msg = deleted.getMessage();
            MessageChannelUnion deletedChannel = deleted.getChannel();
            Guild guild = deleted.getGuild();
            String attachments = deleted.getMessage().getAttachments().stream().map(a-> "  - " + a.getFileName() + " : " + a.getUrl()).collect(Collectors.joining("\n"));
            String stickers = deleted.getMessage().getStickers().stream().map(a-> "  - " + a.getName() + " : " + a.getIconUrl()).collect(Collectors.joining("\n"));

            logger.info(msg.getAuthor().getGlobalName()+ "'s message has been deleted in ("+deletedChannel.getName()+") on guild ("+guild.getName()+") Original content: (" + msg.getContentStripped() + ")"
                    + ((deleted.getEdited() != null && deleted.getEdited().getContentStripped().length() > 0) ? " (Edited: "+ deleted.getEdited().getContentStripped() +")" : "")
                    + ((attachments.length() > 0) ? "\nAttachments:\n" + attachments : "")
                    + ((stickers.length() > 0) ? "\nStickers:\n" + stickers : "")
            );
            messageLog.remove(e.getMessageIdLong());
        }


        //Check that the channel is not voiceChannel for the next steps
        //This is done because channel can not be converted to threadContainer
        //if the channel is VoiceChannel or text channel that has been integrated to voice channel
        if (!(channel instanceof VoiceChannel)) {

            //This is for cleaning up old threads that are not used anymore or are closed etc
            IThreadContainer threadContainer = channel.asThreadContainer();
            threadContainer.getThreadChannels().forEach(thread -> {
                thread.retrieveParentMessage().queue(msg-> {}, (err)-> {
                    if (err.getMessage().contains("Unknown Message")) {
                        thread.delete().reason("Parent message unknown, Deleted?").queue();
                        logger.warn("Deleting thread "+thread.getName()+" ("+thread.getId()+") on channel "+channel.getName()+" ("+channel.getId()+")) because parent message is unknown!");
                    }
                });
            });
        }

    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent e) {
        if(e.getUser() != null && (e.getUser().isBot() || e.getUser().isSystem())) return;

        if (e.getChannelType().equals(ChannelType.TEXT)) {
            TextChannel channel = e.getChannel().asTextChannel();
            channel.retrieveMessageById(e.getMessageIdLong()).queue(msg->{
                String user = e.getUser() != null ? e.getUser().getGlobalName() : e.getUserId();
                EmojiUnion emoji = e.getReaction().getEmoji();
                logger.info(user+ " added reaction "+emoji.getAsReactionCode()+" to message ["+msg.getId()+"]("+msg.getContentStripped()+") at channel ("+channel.getName()+") in guild ("+e.getGuild().getName()+")");
            });
        }
    }


    @Override
    public void onGuildVoiceVideo(GuildVoiceVideoEvent e) {
        if(e.getMember().getUser().isBot() || e.getMember().getUser().isSystem()) return;

        String status = e.isSendingVideo() ? "started" : "ended";
        logger.info(e.getMember().getUser().getGlobalName() + " " + status + " video in guild (" + e.getGuild().getName() + ")");
    }

}
