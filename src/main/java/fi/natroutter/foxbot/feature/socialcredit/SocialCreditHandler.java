package fi.natroutter.foxbot.feature.socialcredit;

import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SocialCreditHandler {

    private final MongoHandler mongo = FoxBot.getMongo();
    private final FoxLogger logger = FoxBot.getLogger();
    private final Config config = FoxBot.getConfig().get();
    private final BotHandler bot = FoxBot.getBotHandler();

    public final ConcurrentHashMap<String, LocalDateTime> joinTimes = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Boolean> joinRewarded = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, LocalDateTime> VoiceTimeCountter = new ConcurrentHashMap<>();

    public final List<String> usersInVoice = new ArrayList<>();

    public SocialCreditHandler() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!bot.isRunning()) return;
                JDA jda = bot.getJDA();

                usersInVoice.forEach((id)-> {

                    //reward user after being in  voice channel for x seconds (only once)
                    if (!joinRewarded.containsKey(id)) {
                        User target = jda.getUserById(id);
                        if (target == null) {
                            logger.warn("Failed to get user by id: " + id);
                            return;
                        }

                        Duration dura = Duration.between(joinTimes.get(id), LocalDateTime.now());

                        if (dura.toMinutes() >= config.getSocialCredits().getMinVoiceTime()) { // if in channel more than x minutes!
                            logger.info("Rewarding "+target.getGlobalName()+" with 2 social credits for being in voice channel for more than "+config.getSocialCredits().getMinVoiceTime()+" minutes");
                            add(target, 2);
                            joinRewarded.put(id, true);
                        }
                    }

                    //Reward user every x minutes for beeing in voice channel
                    if (VoiceTimeCountter.containsKey(id)) {
                        Duration dura2 = Duration.between(VoiceTimeCountter.get(id), LocalDateTime.now());

                        if (dura2.toMinutes() >= config.getSocialCredits().getRewardInterval()) {
                            User target = jda.getUserById(id);
                            if (target == null) {
                                logger.warn("Failed to get user by id: " + id);
                                return;
                            }
                            logger.info("Rewarding "+target.getGlobalName()+" with 5 social credits for being in voice channel for 1 hour");
                            add(target, 5);
                            VoiceTimeCountter.put(id, LocalDateTime.now());
                        }
                    } else {
                        VoiceTimeCountter.put(id, LocalDateTime.now());
                    }
                });
            }
        }, 0, 1000);
    }

    public void connected(JDA jda) {
        for(Guild guild : jda.getGuilds()) {
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
    }

    public static boolean useSocialCredits(Config config, Channel chan) {
        if (config.getSocialCredits().isUseAllChannels()) {
            return true;
        }
        return config.getSocialCredits().getChannels().contains(chan.getIdLong());
    }

    /*

    Credit balance editing methods below!

     */

    public void set(User user, int amount) {
        mongo.getUsers().findByID(user.getId(), entry -> {
            entry.setSocialCredits(amount);
            mongo.save(entry);
        });
    }

    public void add(User user, int amount) {
        mongo.getUsers().findByID(user.getId(), entry -> {
            entry.setSocialCredits(entry.getSocialCredits() + amount);
            mongo.save(entry);
        });
    }

    public void take(User user, int amount) {
        mongo.getUsers().findByID(user.getId(), entry -> {
            entry.setSocialCredits(entry.getSocialCredits() - amount);
            mongo.save(entry);
        });
    }

    public void get(User user, Consumer<Long> action) {
        mongo.getUsers().findByID(user.getId(), data -> {
            action.accept(data.getSocialCredits());
        });
    }

    public void top10(User user, Consumer<List<UserEntry>> action) {
        mongo.getUsers().getTopSocial(user.getId(), action);
    }

}
