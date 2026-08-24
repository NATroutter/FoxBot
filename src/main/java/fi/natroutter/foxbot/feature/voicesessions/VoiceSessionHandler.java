package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceSessionHandler {

    public static final long MIN_DURATION_SECONDS = 60;

    /** How often running sessions are checkpointed to Mongo. */
    private static final long CHECKPOINT_INTERVAL_MILLIS = 60_000;

    /**
     * How stale a checkpoint may be and still be picked back up on startup.
     *
     * <p>Comfortably more than a restart plus one checkpoint interval, so an ordinary restart always
     * continues, and short enough that a real outage does not. The session's own clock runs across
     * the gap — the channel was occupied the whole time, the bot just was not watching — so this
     * also bounds how much unwatched time a session length can include.
     */
    private static final long RESUME_GRACE_MILLIS = 5 * 60_000;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();
    private static final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "voice-session-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final ConcurrentMap<Long, ActiveVoiceSession> activeSessions = new ConcurrentHashMap<>();
    private final MongoHandler mongo = FoxBot.getMongo();
    private final FoxLogger logger = FoxBot.getLogger();
    private final VoiceSessionRewards rewards = new VoiceSessionRewards();

    public VoiceSessionHandler() {
        new Timer("voice-session-checkpoint", true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkpointActiveSessions();
            }
        }, CHECKPOINT_INTERVAL_MILLIS, CHECKPOINT_INTERVAL_MILLIS);
    }

    public static ExecutorService worker() {
        return worker;
    }

    /**
     * Writes every running session to Mongo as an active checkpoint, so long sessions show up in
     * the leaderboard before they end and survive a crash. Each write is an upsert on
     * {@code sessionID}, so the eventual final save replaces the checkpoint rather than adding a
     * second row.
     */
    private void checkpointActiveSessions() {
        long now = System.currentTimeMillis();
        List<VoiceSessionEntry> snapshots = activeSessions.values().stream()
                .map(session -> session.snapshot(now))
                .filter(entry -> entry.getDurationSeconds() >= MIN_DURATION_SECONDS)
                .toList();

        if (snapshots.isEmpty()) {
            return;
        }

        worker.execute(() -> {
            try {
                mongo.getVoiceSessions().saveAll(snapshots);
                logger.info("Voice sessions checkpointed", new LogData("Sessions", snapshots.size()));
            } catch (Exception e) {
                logger.error("Failed to checkpoint active voice sessions", e);
            }
        });
    }

    public void connected(JDA jda) {
        // On the worker because it queries Mongo, which blocks.
        worker.execute(() -> {
            try {
                startupScan(jda);
            } catch (Exception e) {
                logger.error("Voice session startup scan failed", e);
            }
        });
    }

    /**
     * Picks up where the previous run left off.
     *
     * <p>A channel that is still occupied and has a fresh enough checkpoint continues that session
     * — same ID, same start, everyone's banked time intact. Everything else starts clean, and any
     * checkpoint that was not resumed is closed so it stops showing as live.
     */
    private void startupScan(JDA jda) {
        long now = System.currentTimeMillis();
        Map<Long, VoiceSessionEntry> resumable = resumableByChannel(now);

        List<String> resumed = new ArrayList<>();
        int started = 0;

        for (Guild guild : jda.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                List<Member> members = nonBotMembers(channel.getMembers());
                if (members.isEmpty()) {
                    continue;
                }

                VoiceSessionEntry checkpoint = resumable.get(channel.getIdLong());
                ActiveVoiceSession session = checkpoint == null
                        ? new ActiveVoiceSession(nextSessionID(), guild.getIdLong(), channel.getIdLong(),
                                channel.getName(), now)
                        : ActiveVoiceSession.resume(checkpoint, channel.getName());

                // A join event can land before the scan reaches its channel; that session wins.
                if (activeSessions.putIfAbsent(channel.getIdLong(), session) != null) {
                    continue;
                }
                members.forEach(member -> session.openSegment(
                        member.getId(), member.getEffectiveName(), member.getEffectiveAvatarUrl(), now));

                if (checkpoint == null) {
                    started++;
                    logger.info("Voice session started on startup scan",
                            new LogChannel(channel),
                            new LogData("SessionID", session.sessionID()),
                            new LogData("Members", members.size()),
                            new LogData("Note", "Started from bot startup time, offline time is not counted")
                    );
                    continue;
                }

                resumed.add(session.sessionID());
                logger.info("Voice session resumed after restart",
                        new LogChannel(channel),
                        new LogData("SessionID", session.sessionID()),
                        new LogData("Members", members.size()),
                        new LogData("Running", formatDuration((now - session.startedAt()) / 1000)),
                        new LogData("Gap", formatDuration((now - checkpoint.getEndedAt()) / 1000))
                );
            }
        }

        mongo.getVoiceSessions().closeOrphanedActive(resumed, closed -> {
            if (closed > 0) {
                logger.warn("Closed voice sessions left active by the previous run",
                        new LogData("Sessions", closed));
            }
        });
        logger.info("Voice session startup scan complete",
                new LogData("Resumed", resumed.size()),
                new LogData("SessionsStarted", started)
        );
    }

    /**
     * The newest checkpoint per channel that is still worth continuing.
     *
     * <p>Only records left active qualify: a session that ended because everyone left is closed,
     * so it can never be reopened by someone walking back into the channel after a restart.
     */
    private Map<Long, VoiceSessionEntry> resumableByChannel(long now) {
        Map<Long, VoiceSessionEntry> resumable = new HashMap<>();
        mongo.getVoiceSessions().findActive(stored -> {
            for (VoiceSessionEntry entry : stored) {
                if (now - entry.getEndedAt() > RESUME_GRACE_MILLIS) {
                    continue;
                }
                // Sorted newest checkpoint first, so the first hit for a channel is the one to keep.
                resumable.putIfAbsent(entry.getChannelID(), entry);
            }
        });
        return resumable;
    }

    public void joined(AudioChannel channel, Member member) {
        long now = System.currentTimeMillis();
        boolean[] created = {false};
        ActiveVoiceSession session = activeSessions.computeIfAbsent(channel.getIdLong(), id -> {
            created[0] = true;
            return new ActiveVoiceSession(
                    nextSessionID(),
                    channel.getGuild().getIdLong(),
                    channel.getIdLong(),
                    channel.getName(),
                    now
            );
        });
        session.openSegment(member.getId(), member.getEffectiveName(), member.getEffectiveAvatarUrl(), now);

        if (created[0]) {
            logger.info("Voice session started",
                    new LogChannel(channel),
                    new LogMember(member),
                    new LogData("SessionID", session.sessionID())
            );
            return;
        }

        logger.info("Voice session member joined",
                new LogChannel(channel),
                new LogMember(member),
                new LogData("SessionID", session.sessionID()),
                new LogData("Members", session.participantCount())
        );
    }

    public void left(AudioChannel channel, Member member) {
        long now = System.currentTimeMillis();
        ActiveVoiceSession session = activeSessions.get(channel.getIdLong());
        if (session == null) {
            return;
        }

        session.closeSegment(member.getId(), now);
        if (hasRemainingNonBotMember(channel, member)) {
            logger.info("Voice session member left",
                    new LogChannel(channel),
                    new LogMember(member),
                    new LogData("SessionID", session.sessionID())
            );
            return;
        }

        activeSessions.remove(channel.getIdLong(), session);
        VoiceSessionEntry finished = session.finish(now);

        logger.info("Voice session ended",
                new LogChannel(channel),
                new LogMember("LastMember", member),
                new LogData("SessionID", finished.getSessionID()),
                new LogData("Duration", formatDuration(finished.getDurationSeconds())),
                new LogData("Participants", finished.getParticipants().size())
        );

        // Off the JDA thread: settling writes to Mongo and renders a card that fetches avatars.
        worker.execute(() -> {
            try {
                finishSession(channel, finished);
            } catch (Exception e) {
                logger.error("Failed to finish voice session " + finished.getSessionID(), e);
            }
        });
    }

    /**
     * Pays the podium, stores the session, then announces it.
     *
     * <p>The order matters: settling writes each winner's credits onto the record, so it has to
     * happen before the record is stored for a detail view to be able to show them.
     */
    private void finishSession(AudioChannel channel, VoiceSessionEntry finished) {
        List<VoiceSessionRewards.Payout> payouts = rewards.settle(channel, finished);
        persistIfLongEnough(finished);

        if (!payouts.isEmpty() && channel instanceof GuildMessageChannel chat) {
            rewards.post(chat, finished, payouts);
        }
    }

    /** Live snapshot of whatever is running in a channel, or null when nothing is. */
    public VoiceSessionEntry activeSnapshot(long channelID) {
        ActiveVoiceSession session = activeSessions.get(channelID);
        return session == null ? null : session.snapshot(System.currentTimeMillis());
    }

    /** Live snapshot of one session by ID, or null once it has ended. */
    public VoiceSessionEntry activeSnapshot(String sessionID) {
        long now = System.currentTimeMillis();
        for (ActiveVoiceSession session : activeSessions.values()) {
            if (session.sessionID().equals(sessionID)) {
                return session.snapshot(now);
            }
        }
        return null;
    }

    public List<VoiceSessionEntry> activeSnapshots() {
        long now = System.currentTimeMillis();
        return activeSessions.values().stream()
                .map(session -> session.snapshot(now))
                .sorted(Comparator.comparingLong(VoiceSessionEntry::getDurationSeconds).reversed())
                .toList();
    }

    /**
     * Writes every running session out as a final checkpoint, still flagged active.
     *
     * <p>Deliberately not marked finished: a session interrupted by the bot stopping is not over,
     * the people are still sitting in the channel. Leaving it active is what lets the next startup
     * tell it apart from a session that ended because everyone left, and continue it.
     */
    public void flushActiveSessions() {
        List<VoiceSessionEntry> pending = drainAsCheckpoints();
        if (pending.isEmpty()) {
            logger.info("Voice session shutdown flush: nothing to save");
            return;
        }
        try {
            // Upsert, not insert: a checkpoint may already hold a row for these sessions.
            mongo.getVoiceSessions().saveAll(pending);
            logger.info("Voice sessions flushed on shutdown", new LogData("Sessions", pending.size()));
        } catch (Exception e) {
            logger.error("Failed to flush active voice sessions", e);
        }
    }

    private List<VoiceSessionEntry> drainAsCheckpoints() {
        long now = System.currentTimeMillis();
        List<VoiceSessionEntry> pending = new ArrayList<>();
        activeSessions.forEach((channelID, session) -> {
            if (!activeSessions.remove(channelID, session)) {
                return;
            }
            VoiceSessionEntry checkpoint = session.snapshot(now);
            if (checkpoint.getDurationSeconds() >= MIN_DURATION_SECONDS) {
                pending.add(checkpoint);
            }
        });
        return pending;
    }

    private void persistIfLongEnough(VoiceSessionEntry finished) {
        if (finished.getDurationSeconds() < MIN_DURATION_SECONDS) {
            logger.info("Voice session discarded as too short",
                    new LogData("SessionID", finished.getSessionID()),
                    new LogData("Channel", finished.getChannelName()),
                    new LogData("Duration", finished.getDurationSeconds() + "s"),
                    new LogData("Minimum", MIN_DURATION_SECONDS + "s")
            );
            return;
        }
        // Already on the worker: the caller settled the payout first so the credits go out with it.
        try {
            mongo.save(finished);
            logger.info("Voice session saved",
                    new LogData("SessionID", finished.getSessionID()),
                    new LogData("Channel", finished.getChannelName()),
                    new LogData("Duration", formatDuration(finished.getDurationSeconds())),
                    new LogData("Participants", finished.getParticipants().size())
            );
        } catch (Exception e) {
            logger.error("Failed to save voice session " + finished.getSessionID(), e);
        }
    }

    private static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    private static List<Member> nonBotMembers(List<Member> members) {
        return members.stream()
                .filter(member -> !member.getUser().isBot() && !member.getUser().isSystem())
                .toList();
    }

    private static boolean hasRemainingNonBotMember(AudioChannel channel, Member leavingMember) {
        return channel.getMembers().stream()
                .anyMatch(member -> member.getIdLong() != leavingMember.getIdLong()
                        && !member.getUser().isBot()
                        && !member.getUser().isSystem());
    }

    private static String nextSessionID() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }
}
