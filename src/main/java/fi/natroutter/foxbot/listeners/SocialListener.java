package fi.natroutter.foxbot.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Wakeup;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SocialListener extends ListenerAdapter {

    private CreditHandler credit = FoxBot.getCreditHandler();
    private FoxLogger logger = FoxBot.getLogger();
    private Config config = FoxBot.getConfig().get();

    private ConcurrentHashMap<String, LocalDateTime> joinTimes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> joinRewarded = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LocalDateTime> VoiceTimeCountter = new ConcurrentHashMap<>();

    private List<String> usersInVoice = new ArrayList<>();

    private static JDA jda() { return FoxBot.getBot().getJDA(); }

    public SocialListener() {

        for(Guild guild : jda().getGuilds()) {
            for (Member member : guild.getMembers()) {
                if (member.getVoiceState() == null) continue;

                if (member.getVoiceState().inAudioChannel() && member.getVoiceState().getChannel() != null) {

                    long channelID = member.getVoiceState().getChannel().getIdLong();
                    if (!config.getSocialCredits().isUseAllChannels()) {
                        if (!config.getSocialCredits().getChannels().contains(channelID)) {
                            continue;
                        }
                    }

                    joinTimes.put(member.getId(), LocalDateTime.now().plusSeconds(config.getSocialCredits().getMinVoiceTime() + 1));
                    joinRewarded.put(member.getId(), true);
                    usersInVoice.add(member.getId());
                }
            }
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {

                usersInVoice.forEach((id)-> {

                    //reward user after being in  voice channel for x seconds (only once)
                    if (!joinRewarded.containsKey(id)) {
                        User target = jda().getUserById(id);
                        if (target == null) {
                            logger.warn("Failed to get user by id: " + id);
                            return;
                        }

                        Duration dura = Duration.between(joinTimes.get(id), LocalDateTime.now());

                        if (dura.toMinutes() >= config.getSocialCredits().getMinVoiceTime()) { // if in channel more than x minutes!
                            logger.info("Rewarding "+target.getGlobalName()+" with 2 social credits for being in voice channel for more than "+config.getSocialCredits().getMinVoiceTime()+" minutes");
                            credit.add(target, 2);
                            joinRewarded.put(id, true);
                        }
                    }

                    //Reward user every x minutes for beeing in voice channel
                    if (VoiceTimeCountter.containsKey(id)) {
                        Duration dura2 = Duration.between(VoiceTimeCountter.get(id), LocalDateTime.now());

                        if (dura2.toMinutes() >= config.getSocialCredits().getRewardInterval()) {
                            User target = jda().getUserById(id);
                            if (target == null) {
                                logger.warn("Failed to get user by id: " + id);
                                return;
                            }
                            logger.info("Rewarding "+target.getGlobalName()+" with 5 social credits for being in voice channel for 1 hour");
                            credit.add(target, 5);
                            VoiceTimeCountter.put(id, LocalDateTime.now());
                        }
                    } else {
                        VoiceTimeCountter.put(id, LocalDateTime.now());
                    }
                });
            }
        }, 0, 1000);
    }

    public static boolean useSocialCredits(Config config,MessageChannelUnion chan) {
        if (config.getSocialCredits().isUseAllChannels()) {
            return true;
        }
        if (config.getSocialCredits().getChannels().contains(chan.getIdLong())) {
            return true;
        }
        return false;
    }
    public static boolean useSocialCredits(Config config,AudioChannelUnion chan) {
        if (config.getSocialCredits().isUseAllChannels()) {
            return true;
        }
        if (config.getSocialCredits().getChannels().contains(chan.getIdLong())) {
            return true;
        }
        return false;
    }


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

            if (useSocialCredits(config,chan)) {
                credit.add(e.getAuthor(), 10);
                logger.info(e.getAuthor().getGlobalName() + " sent an image to foodStash, giving 10 social credits");
            }

            return;
        }

        if (useSocialCredits(config,chan)) {
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

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
        if(e.getMember().getUser().isBot() || e.getMember().getUser().isSystem()) return;

        if (e.getChannelJoined() != null && e.getChannelLeft() != null) {
            if(!e.getChannelJoined().getId().equals(e.getChannelLeft().getId())) {
                if (Wakeup.users.contains(e.getEntity().getIdLong())) return;
                return;
            }
        }

        if (e.getChannelJoined() != null) {
            if (!useSocialCredits(config,e.getChannelJoined())) {
                return;
            }
        }
        if (e.getChannelLeft() != null) {
            if (!useSocialCredits(config,e.getChannelLeft())) {
                return;
            }
        }

        if (e.getChannelJoined() == null) {
            usersInVoice.remove(e.getMember().getId());
            joinRewarded.remove(e.getMember().getId());
            VoiceTimeCountter.remove(e.getMember().getId());

            //Voice leave too early penalty
            if (joinTimes.containsKey(e.getMember().getId())) {
                LocalDateTime joinTime = joinTimes.get(e.getMember().getId());
                LocalDateTime now = LocalDateTime.now();
                long diff = Duration.between(joinTime, now).toSeconds();
                if (diff < config.getSocialCredits().getMinVoiceTime()) {
                    logger.warn(e.getMember().getUser().getGlobalName() + " left voice channel too early, removing 5 social credit");
                    credit.take(e.getMember().getUser(), 5);
                    return;
                }
            }
            return;
        }

        //Voice join reward!
        if (e.getChannelLeft() == null) {
            Config.SocialCredits social = config.getSocialCredits();
            if (social.isUseAllChannels()) {
                joinTimes.put(e.getMember().getId(), LocalDateTime.now());
                usersInVoice.add(e.getMember().getId());
            } else {
                if (social.getChannels().contains(e.getChannelJoined().getIdLong())) {
                    joinTimes.put(e.getMember().getId(), LocalDateTime.now());
                    usersInVoice.add(e.getMember().getId());
                }
            }
        }
    }
}
