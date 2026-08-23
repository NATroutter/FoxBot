package fi.natroutter.foxbot.commands.vstats;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer;

import java.util.ArrayList;
import java.util.List;

public class VoiceSessionView {

    public static final int VIEW_LIMIT = 25;

    /**
     * What the view shows, which decides its renderer, its page size, and whether per-session
     * detail buttons are offered.
     */
    public enum Kind {
        /** Ranked one-line cards, each openable into a detail view. */
        LEADERBOARD(VoiceSessionImageRenderer.TOP_PAGE_SIZE, true),
        /** Running sessions, each already rendered as a full block, so no detail to open into. */
        CURRENT(VoiceSessionImageRenderer.CURRENT_PAGE_SIZE, false);

        private final int pageSize;
        private final boolean detailsEnabled;

        Kind(int pageSize, boolean detailsEnabled) {
            this.pageSize = pageSize;
            this.detailsEnabled = detailsEnabled;
        }
    }

    private final String id;
    private final long guildID;
    private final long userID;
    private final String title;
    private final Kind kind;
    private final List<VoiceSessionEntry> sessions;
    private final long expiresAt;
    private int currentPage;
    private int detailIndex = -1;

    VoiceSessionView(String id, long guildID, long userID, String title, Kind kind,
                     List<VoiceSessionEntry> sessions, long expiresAt) {
        this.id = id;
        this.guildID = guildID;
        this.userID = userID;
        this.title = title;
        this.kind = kind;
        this.sessions = new ArrayList<>(sessions);
        this.expiresAt = expiresAt;
    }

    public Kind kind() {
        return kind;
    }

    public int pageSize() {
        return kind.pageSize;
    }

    /**
     * Whether per-session detail buttons are offered. The leaderboard uses them; the live view
     * does not, so it carries pagination only.
     */
    public boolean detailsEnabled() {
        return kind.detailsEnabled;
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
        return Math.max(1, (sessions.size() + pageSize() - 1) / pageSize());
    }

    public List<VoiceSessionEntry> pageSessions() {
        int start = currentPage * pageSize();
        int end = Math.min(start + pageSize(), sessions.size());
        if (start >= end) {
            return List.of();
        }
        return new ArrayList<>(sessions.subList(start, end));
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
