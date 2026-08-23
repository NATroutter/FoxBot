package fi.natroutter.foxbot.commands.vstats;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VoiceSessionViewStore {

    private static final VoiceSessionViewStore INSTANCE = new VoiceSessionViewStore();
    private static final Duration VIEW_TTL = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final ConcurrentMap<String, VoiceSessionView> views = new ConcurrentHashMap<>();

    public static VoiceSessionViewStore get() {
        return INSTANCE;
    }

    public VoiceSessionView create(long guildID, long userID, String title, boolean detailsEnabled,
                                   List<fi.natroutter.foxbot.database.models.VoiceSessionEntry> sessions) {
        cleanupExpired();

        String id;
        do {
            byte[] bytes = new byte[8];
            RANDOM.nextBytes(bytes);
            id = HEX.formatHex(bytes);
        } while (views.containsKey(id));

        VoiceSessionView view = new VoiceSessionView(
                id,
                guildID,
                userID,
                title,
                detailsEnabled,
                sessions,
                System.currentTimeMillis() + VIEW_TTL.toMillis()
        );
        views.put(id, view);
        return view;
    }

    public Optional<VoiceSessionView> find(String id) {
        VoiceSessionView view = views.get(id);
        if (view == null) {
            return Optional.empty();
        }
        if (view.isExpired()) {
            remove(id);
            return Optional.empty();
        }
        return Optional.of(view);
    }

    public void remove(String id) {
        views.remove(id);
    }

    public void cleanupExpired() {
        views.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
