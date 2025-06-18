package fi.natroutter.foxbot.feature.socialcredit.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Wakeup;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.feature.socialcredit.SocialCreditHandler;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;
import java.time.LocalDateTime;

public class SocialMessageUpdateListener extends ListenerAdapter {

    private final SocialCreditHandler credit = FoxBot.getSocialCreditHandler();
    private final FoxLogger logger = FoxBot.getLogger();
    private final Config config = FoxBot.getConfig().get();

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
            if (!SocialCreditHandler.useSocialCredits(config,e.getChannelJoined())) {
                return;
            }
        }
        if (e.getChannelLeft() != null) {
            if (!SocialCreditHandler.useSocialCredits(config,e.getChannelLeft())) {
                return;
            }
        }

        if (e.getChannelJoined() == null) {
            credit.usersInVoice.remove(e.getMember().getId());
            credit.joinRewarded.remove(e.getMember().getId());
            credit.VoiceTimeCountter.remove(e.getMember().getId());

            //Voice leave too early penalty
            if (credit.joinTimes.containsKey(e.getMember().getId())) {
                LocalDateTime joinTime = credit.joinTimes.get(e.getMember().getId());
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
                credit.joinTimes.put(e.getMember().getId(), LocalDateTime.now());
                credit.usersInVoice.add(e.getMember().getId());
            } else {
                if (social.getChannels().contains(e.getChannelJoined().getIdLong())) {
                    credit.joinTimes.put(e.getMember().getId(), LocalDateTime.now());
                    credit.usersInVoice.add(e.getMember().getId());
                }
            }
        }
    }

}
