package fi.natroutter.foxbot.database.models;

import fi.natroutter.foxlib.mongo.MongoData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class VoiceSessionEntry implements MongoData {

    String sessionID;
    long guildID;
    long channelID;
    String channelName;

    long startedAt;
    long endedAt;
    long durationSeconds;

    /**
     * True while the session is still running and this record is only a periodic checkpoint.
     * Checkpoints are upserted under the same {@code sessionID}, so the final write replaces them.
     * {@code endedAt} on an active record is the checkpoint time, not a real end time.
     */
    boolean active;

    List<VoiceParticipant> participants;

    @Override
    public String id() {
        return sessionID;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class VoiceParticipant {
        String userID;
        String username;
        long totalSeconds;
        List<VoiceSegment> segments;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class VoiceSegment {
        long joinedAt;
        long leftAt;
        long durationSeconds;
    }
}
