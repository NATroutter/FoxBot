package fi.natroutter.foxbot.commands.sticker;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class StickerPickerSessionStore {

    private static final Duration SESSION_TTL = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final ConcurrentMap<String, StickerPickerSession> sessions = new ConcurrentHashMap<>();

    StickerPickerSession create(long userId, long channelId, List<String> stickerIds) {
        cleanupExpired();

        String id;
        do {
            byte[] bytes = new byte[8];
            RANDOM.nextBytes(bytes);
            id = HEX.formatHex(bytes);
        } while (sessions.containsKey(id));

        StickerPickerSession session = new StickerPickerSession(
                id,
                userId,
                channelId,
                stickerIds,
                System.currentTimeMillis() + SESSION_TTL.toMillis()
        );
        sessions.put(id, session);
        return session;
    }

    Optional<StickerPickerSession> find(String sessionId) {
        StickerPickerSession session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired()) {
            sessions.remove(sessionId, session);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    void cleanupExpired() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
