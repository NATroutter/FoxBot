package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.socialcredit.SocialCreditHandler;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pays the podium of a finished voice session in social credits and posts the result into the
 * channel's own text chat.
 *
 * <p>The thresholds keep this from being farmable: a session has to be a real hangout, and a
 * participant has to have actually been in it, not just dropped by at the end. The payout scales
 * with how long the session ran, so a whole evening in voice is worth more than a short call
 * — first place in a six hour session earns six times what first place in a one hour session does.
 *
 * <p>Only sessions that end naturally — the last person leaves — pay out. A session cut short by
 * the bot shutting down is not over from the users' point of view, so it is left alone.
 */
public class VoiceSessionRewards {

    /** A session shorter than this pays nothing at all. */
    public static final long MIN_SESSION_SECONDS = 30 * 60;

    /** A participant below this is not on the podium, however few others there were. */
    public static final long MIN_PARTICIPANT_SECONDS = 10 * 60;

    /** Credits for first, second and third place, before the session-length multiplier. */
    static final int[] PLACE_CREDITS = {10, 5, 3};

    private static final AtomicLong IMAGE_SEQUENCE = new AtomicLong();
    private static final String EMBED_TITLE = "Voice Session Rewards";

    private final SocialCreditHandler socialCredits = FoxBot.getSocialCreditHandler();
    private final FoxLogger logger = FoxBot.getLogger();
    private final Config config = FoxBot.getConfigProvider().get();
    private final VoiceSessionImageRenderer renderer = new VoiceSessionImageRenderer();

    /** One podium place: who, where they placed, and what it paid. */
    public record Payout(VoiceSessionEntry.VoiceParticipant participant, int place, int credits) {
    }

    /** How many places are paid. */
    public static int places() {
        return PLACE_CREDITS.length;
    }

    /** What a place pays before the session-length multiplier, for {@code place} counted from 1. */
    public static int baseCredits(int place) {
        return PLACE_CREDITS[place - 1];
    }

    /** Whole hours of session, never below one — a 45 minute session still pays the base amount. */
    public static int multiplier(long sessionSeconds) {
        return (int) Math.max(1, sessionSeconds / 3600);
    }

    /** The podium, or an empty list when the session or its participants did not clear the bar. */
    public static List<Payout> payouts(VoiceSessionEntry session) {
        if (session.getDurationSeconds() < MIN_SESSION_SECONDS || session.getParticipants() == null) {
            return List.of();
        }

        List<VoiceSessionEntry.VoiceParticipant> eligible = session.getParticipants().stream()
                .filter(participant -> participant.getTotalSeconds() >= MIN_PARTICIPANT_SECONDS)
                .sorted(Comparator.comparingLong(VoiceSessionEntry.VoiceParticipant::getTotalSeconds).reversed())
                .limit(PLACE_CREDITS.length)
                .toList();

        int multiplier = multiplier(session.getDurationSeconds());
        List<Payout> payouts = new ArrayList<>(eligible.size());
        for (int index = 0; index < eligible.size(); index++) {
            payouts.add(new Payout(eligible.get(index), index + 1, PLACE_CREDITS[index] * multiplier));
        }
        return payouts;
    }

    /**
     * Credits the podium and announces it. Does nothing when nobody qualified, or when the channel
     * is outside the social credit whitelist — the same channels the per-minute voice rewards use.
     */
    public void award(AudioChannel channel, VoiceSessionEntry session) {
        List<Payout> payouts = payouts(session);
        if (payouts.isEmpty()) {
            return;
        }
        if (!SocialCreditHandler.useSocialCredits(config, channel)) {
            logger.info("Voice session payout skipped: channel is not a social credit channel",
                    new LogData("SessionID", session.getSessionID()),
                    new LogData("Channel", session.getChannelName())
            );
            return;
        }

        for (Payout payout : payouts) {
            socialCredits.add(payout.participant().getUserID(), payout.credits());
            logger.info("Voice session payout",
                    new LogData("SessionID", session.getSessionID()),
                    new LogData("Channel", session.getChannelName()),
                    new LogData("Place", "#" + payout.place()),
                    new LogData("User", payout.participant().getUsername()),
                    new LogData("Credits", payout.credits())
            );
        }

        if (channel instanceof GuildMessageChannel chat) {
            post(chat, session, payouts);
        }
    }

    /** Renders and sends the payout card without awarding anything, for the console debug command. */
    public void preview(GuildMessageChannel channel, VoiceSessionEntry session) {
        post(channel, session, payouts(session));
    }

    /** The one place a payout card is sent, so a preview is the same message a real payout posts. */
    private void post(GuildMessageChannel channel, VoiceSessionEntry session, List<Payout> payouts) {
        if (payouts.isEmpty()) {
            return;
        }
        try {
            byte[] png = renderer.renderPayout(session, payouts);
            // Discord caches attachments by name, so every card gets its own.
            String fileName = "vstats-payout-" + IMAGE_SEQUENCE.incrementAndGet() + ".png";

            MessageEmbed embed = new EmbedBuilder()
                    .setTitle(EMBED_TITLE)
                    .setColor(config.getThemeColor().asColor())
                    .setImage("attachment://" + fileName)
                    .build();

            channel.sendMessageEmbeds(embed)
                    .setFiles(FileUpload.fromData(png, fileName))
                    .queue(null, error ->
                    logger.warn("Failed to post voice session payout card",
                            new LogData("Channel", channel.getName()),
                            new LogData("Reason", error.getMessage())
                    ));
        } catch (IOException e) {
            logger.error("Failed to render voice session payout for session " + session.getSessionID(), e);
        }
    }
}
