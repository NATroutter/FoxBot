package fi.natroutter.foxbot.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Wakeup;
import fi.natroutter.foxbot.configs.Config;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.data.Embeds;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.UserEntry;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SocialListener extends ListenerAdapter {

    private CreditHandler credit = FoxBot.getCreditHandler();
    private MongoHandler mongo = FoxBot.getMongo();
    private FoxLogger logger = FoxBot.getLogger();
    private ConfigProvider config = FoxBot.getConfig();

    private ConcurrentHashMap<String, LocalDateTime> joinTimes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> joinRewarded = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LocalDateTime> VoiceTimeCountter = new ConcurrentHashMap<>();

    private List<String> usersInVoice = new ArrayList<>();

    private static JDA jda() { return FoxBot.getBot().getJda(); }

    public SocialListener() {

        for(Guild guild : jda().getGuilds()) {
            for (Member member : guild.getMembers()) {
                if (member.getVoiceState() == null) continue;

                if (member.getVoiceState().inAudioChannel()) {
                    joinTimes.put(member.getId(), LocalDateTime.now().plusSeconds(config.get().getChannels().getMinVoiceTime() + 1));
                    joinRewarded.put(member.getId(), true);
                    usersInVoice.add(member.getId());
                }
            }
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {

                usersInVoice.forEach((id)-> {

                    //reward user after beeing in channel for x seconds (only once)
                    if (!joinRewarded.containsKey(id)) {
                        Duration dura = Duration.between(joinTimes.get(id), LocalDateTime.now());

                        if (dura.toMinutes() >= config.get().getChannels().getMinVoiceTime()) { // if in channel more than x minutes!
                            User target = jda().getUserById(id);
                            if (target == null) {
                                logger.warn("Failed to get user by id: " + id);
                                return;
                            }
                            logger.info("Rewarding "+target.getGlobalName()+" with 2 social credits for being in voice channel for more than "+config.get().getChannels().getMinVoiceTime()+" minutes");
                            credit.add(target, 2);
                            joinRewarded.put(id, true);
                        }
                    }

                    //Reward user every x minutes for beeing in voice channel
                    if (VoiceTimeCountter.containsKey(id)) {
                        Duration dura2 = Duration.between(VoiceTimeCountter.get(id), LocalDateTime.now());

                        if (dura2.toMinutes() >= config.get().getChannels().getRewardInterval()) {
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

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        User author = e.getAuthor();
        Message msg = e.getMessage();
        MessageChannelUnion chan = e.getChannel();

        if (author.isBot()) return;

        String foodStashID = config.get().getChannels().getFoodStash();

        if (e.getChannelType().equals(ChannelType.GUILD_PUBLIC_THREAD) && !(
                e.getMessage().getContentRaw().length() > 0 ||
                        e.getMessage().getAttachments().size() > 0 ||
                        e.getMessage().getStickers().size() > 0 ||
                        e.getMessage().getEmbeds().size() > 0
        )) return;

        if (e.getMessage().getType().equals(MessageType.THREAD_CREATED)) return;

        //if channel is foodStash
        if (chan.getId().equalsIgnoreCase(foodStashID) && e.getChannelType().equals(ChannelType.TEXT)) {
            EmbedBuilder em = Utils.error("Attention!", "**Hey** "+author.getAsMention()+"\nOnly food related images are allowed in this channel\n\nIf you want to comment on someones image/post you need to start a thread!");

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
            msg.addReaction(Emoji.fromUnicode("\uD83D\uDC4D")).queue();
            msg.addReaction(Emoji.fromUnicode("\uD83D\uDC4E")).queue();

            credit.add(e.getAuthor(), 10);
            logger.info(e.getAuthor().getGlobalName() + " sent an image to foodStash, giving 10 social credits");

            return;
        }

        //if channel is not foodStash!
        if (msg.getAttachments().size() > 0) {
            credit.add(e.getAuthor(), 2);
            logger.info(e.getAuthor().getGlobalName() + " sent an message with attachment, giving 2 social credits");
        } else {
            credit.add(e.getAuthor(), 1);
            logger.info(e.getAuthor().getGlobalName() + " sent an message, giving 1 social credit");
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
        if (e.getChannelJoined() == null) {
            usersInVoice.remove(e.getMember().getId());
            joinRewarded.remove(e.getMember().getId());
            VoiceTimeCountter.remove(e.getMember().getId());

            //Voice leave too early penalty
            if (joinTimes.containsKey(e.getMember().getId())) {
                LocalDateTime joinTime = joinTimes.get(e.getMember().getId());
                LocalDateTime now = LocalDateTime.now();
                long diff = Duration.between(joinTime, now).toSeconds();
                if (diff < config.get().getChannels().getMinVoiceTime()) {
                    logger.warn(e.getMember().getUser().getGlobalName() + " left voice channel too early, removing 5 social credit");
                    credit.take(e.getMember().getUser(), 5);
                    return;
                }
            }
            return;
        }

        //Voice join reward!
        if (e.getChannelLeft() == null) {
            joinTimes.put(e.getMember().getId(), LocalDateTime.now());
            usersInVoice.add(e.getMember().getId());
        }
    }
}
