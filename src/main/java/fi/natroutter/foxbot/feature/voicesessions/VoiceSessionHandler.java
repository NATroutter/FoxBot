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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
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
        // Anything still flagged active belongs to a previous run that did not shut down cleanly.
        worker.execute(() -> {
            try {
                mongo.getVoiceSessions().closeOrphanedActive(closed -> {
                    if (closed > 0) {
                        logger.warn("Closed voice sessions left active by an unclean shutdown",
                                new LogData("Sessions", closed));
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to close orphaned active voice sessions", e);
            }
        });

        long now = System.currentTimeMillis();
        int started = 0;
        for (Guild guild : jda.getGuilds()) {
            for (VoiceChannel channel : guild.getVoiceChannels()) {
                List<Member> members = nonBotMembers(channel.getMembers());
                if (members.isEmpty()) {
                    continue;
                }

                ActiveVoiceSession session = new ActiveVoiceSession(
                        nextSessionID(),
                        guild.getIdLong(),
                        channel.getIdLong(),
                        channel.getName(),
                        now
                );
                members.forEach(member -> session.openSegment(member.getId(), member.getEffectiveName(), now));
                activeSessions.put(channel.getIdLong(), session);
                started++;

                logger.info("Voice session started on startup scan",
                        new LogChannel(channel),
                        new LogData("SessionID", session.sessionID()),
                        new LogData("Members", members.size()),
                        new LogData("Note", "Started from bot startup time, offline time is not counted")
                );
            }
        }
        logger.info("Voice session startup scan complete", new LogData("SessionsStarted", started));
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
        session.openSegment(member.getId(), member.getEffectiveName(), now);

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

        persistIfLongEnough(finished);
    }

    public List<VoiceSessionEntry> activeSnapshots() {
        long now = System.currentTimeMillis();
        return activeSessions.values().stream()
                .map(session -> session.snapshot(now))
                .sorted(Comparator.comparingLong(VoiceSessionEntry::getDurationSeconds).reversed())
                .toList();
    }

    public void flushActiveSessions() {
        List<VoiceSessionEntry> pending = drainAndClose();
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

    private List<VoiceSessionEntry> drainAndClose() {
        long now = System.currentTimeMillis();
        List<VoiceSessionEntry> pending = new ArrayList<>();
        activeSessions.forEach((channelID, session) -> {
            if (!activeSessions.remove(channelID, session)) {
                return;
            }
            VoiceSessionEntry finished = session.finish(now);
            if (finished.getDurationSeconds() >= MIN_DURATION_SECONDS) {
                pending.add(finished);
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
        worker.execute(() -> {
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
        });
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
