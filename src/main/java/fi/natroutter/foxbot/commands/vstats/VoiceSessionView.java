package fi.natroutter.foxbot.commands.vstats;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer;

import java.util.ArrayList;
import java.util.List;

public class VoiceSessionView {

    public static final int PAGE_SIZE = VoiceSessionImageRenderer.TOP_PAGE_SIZE;
    public static final int VIEW_LIMIT = 25;

    private final String id;
    private final long guildID;
    private final long userID;
    private final String title;
    private final boolean detailsEnabled;
    private final List<VoiceSessionEntry> sessions;
    private final long expiresAt;
    private int currentPage;
    private int detailIndex = -1;

    VoiceSessionView(String id, long guildID, long userID, String title, boolean detailsEnabled,
                     List<VoiceSessionEntry> sessions, long expiresAt) {
        this.id = id;
        this.guildID = guildID;
        this.userID = userID;
        this.title = title;
        this.detailsEnabled = detailsEnabled;
        this.sessions = new ArrayList<>(sessions);
        this.expiresAt = expiresAt;
    }

    /**
     * Whether per-session detail buttons are offered. The leaderboard uses them; the live view
     * does not, so it carries pagination only.
     */
    public boolean detailsEnabled() {
        return detailsEnabled;
    }

    public String id() {
        return id;
    }

    /** Embed title for this view, so paging back does not relabel a "current sessions" view. */
    public String title() {
        return title;
    }

    public long guildID() {
        return guildID;
    }

    public long userID() {
        return userID;
    }

    public List<VoiceSessionEntry> sessions() {
        return sessions;
    }

    public int currentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, Math.min(currentPage, totalPages() - 1));
    }

    public int detailIndex() {
        return detailIndex;
    }

    public void setDetailIndex(int detailIndex) {
        this.detailIndex = detailIndex;
    }

    public int totalPages() {
        return Math.max(1, (sessions.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public List<VoiceSessionEntry> pageSessions() {
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sessions.size());
        if (start >= end) {
            return List.of();
        }
        return new ArrayList<>(sessions.subList(start, end));
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
