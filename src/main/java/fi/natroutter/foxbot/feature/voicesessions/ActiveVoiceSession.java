package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiveVoiceSession {

    private final String sessionID;
    private final long guildID;
    private final long channelID;
    private final String channelName;
    private final long startedAt;
    private final Map<String, ActiveParticipant> participants = new LinkedHashMap<>();

    public ActiveVoiceSession(String sessionID, long guildID, long channelID, String channelName, long startedAt) {
        this.sessionID = sessionID;
        this.guildID = guildID;
        this.channelID = channelID;
        this.channelName = channelName;
        this.startedAt = startedAt;
    }

    /**
     * Rebuilds a running session from its last stored checkpoint, so a restart continues it instead
     * of starting a new one.
     *
     * <p>The session keeps its ID and its original start, and every participant keeps the time they
     * had already banked — a checkpoint stores an in-progress stretch as a finished segment, so
     * restoring the segments as they stand is exactly the time the bot actually saw. Nobody comes
     * back mid-segment: whoever is still in the channel is given a fresh one from the moment the
     * bot returned, which is what keeps the gap out of everyone's totals.
     *
     * @param channelName the channel's name now, which may have changed while the bot was down
     */
    static ActiveVoiceSession resume(VoiceSessionEntry entry, String channelName) {
        ActiveVoiceSession session = new ActiveVoiceSession(
                entry.getSessionID(),
                entry.getGuildID(),
                entry.getChannelID(),
                channelName,
                entry.getStartedAt()
        );

        if (entry.getParticipants() == null) {
            return session;
        }
        for (VoiceSessionEntry.VoiceParticipant stored : entry.getParticipants()) {
            ActiveParticipant participant = new ActiveParticipant(stored.getUserID(), stored.getUsername());
            participant.avatarUrl = stored.getAvatarUrl();
            if (stored.getSegments() != null) {
                participant.segments.addAll(stored.getSegments());
            }
            session.participants.put(stored.getUserID(), participant);
        }
        return session;
    }

    public String sessionID() {
        return sessionID;
    }

    public long channelID() {
        return channelID;
    }

    public String channelName() {
        return channelName;
    }

    public long startedAt() {
        return startedAt;
    }

    public synchronized int participantCount() {
        return participants.size();
    }

    public synchronized void openSegment(String userID, String username, String avatarUrl, long joinedAt) {
        ActiveParticipant participant = participants.computeIfAbsent(userID, id -> new ActiveParticipant(userID, username));
        participant.username = username;
        participant.avatarUrl = avatarUrl;
        if (participant.openJoinedAt == null) {
            participant.openJoinedAt = joinedAt;
        }
    }

    public synchronized void closeSegment(String userID, long leftAt) {
        ActiveParticipant participant = participants.get(userID);
        if (participant == null || participant.openJoinedAt == null) {
            return;
        }
        participant.closeOpenSegment(leftAt);
    }

    public synchronized VoiceSessionEntry finish(long endedAt) {
        return buildEntry(endedAt, true);
    }

    public synchronized VoiceSessionEntry snapshot(long now) {
        return buildEntry(now, false);
    }

    private VoiceSessionEntry buildEntry(long endedAt, boolean closeOpenSegments) {
        List<VoiceSessionEntry.VoiceParticipant> finishedParticipants = new ArrayList<>();
        for (ActiveParticipant participant : participants.values()) {
            List<VoiceSessionEntry.VoiceSegment> segments = new ArrayList<>(participant.segments);
            Long openJoinedAt = participant.openJoinedAt;
            if (openJoinedAt != null) {
                VoiceSessionEntry.VoiceSegment segment = segment(openJoinedAt, endedAt);
                segments.add(segment);
                if (closeOpenSegments) {
                    participant.closeOpenSegment(endedAt);
                }
            }

            long totalSeconds = segments.stream()
                    .mapToLong(VoiceSessionEntry.VoiceSegment::getDurationSeconds)
                    .sum();
            finishedParticipants.add(new VoiceSessionEntry.VoiceParticipant(
                    participant.userID,
                    participant.username,
                    participant.avatarUrl,
                    totalSeconds,
                    segments,
                    0 // Filled in when the session ends and the podium is paid.
            ));
        }

        finishedParticipants.sort(Comparator.comparingLong(VoiceSessionEntry.VoiceParticipant::getTotalSeconds).reversed());

        VoiceSessionEntry entry = new VoiceSessionEntry();
        entry.setSessionID(sessionID);
        entry.setGuildID(guildID);
        entry.setChannelID(channelID);
        entry.setChannelName(channelName);
        entry.setStartedAt(startedAt);
        entry.setEndedAt(endedAt);
        entry.setDurationSeconds(durationSeconds(startedAt, endedAt));
        entry.setActive(!closeOpenSegments);
        entry.setParticipants(finishedParticipants);
        return entry;
    }

    private static VoiceSessionEntry.VoiceSegment segment(long joinedAt, long leftAt) {
        return new VoiceSessionEntry.VoiceSegment(joinedAt, leftAt, durationSeconds(joinedAt, leftAt));
    }

    private static long durationSeconds(long startedAt, long endedAt) {
        return Math.max(0, (endedAt - startedAt) / 1000);
    }

    private static class ActiveParticipant {
        private final String userID;
        private String username;
        private String avatarUrl;
        private Long openJoinedAt;
        private final List<VoiceSessionEntry.VoiceSegment> segments = new ArrayList<>();

        private ActiveParticipant(String userID, String username) {
            this.userID = userID;
            this.username = username;
        }

        private void closeOpenSegment(long leftAt) {
            segments.add(segment(openJoinedAt, leftAt));
            openJoinedAt = null;
        }
    }
}
