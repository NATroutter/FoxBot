package fi.natroutter.foxbot.feature.stickers.data;

import fi.natroutter.foxbot.feature.stickers.listeners.StickerPickerListener;

import java.util.List;

public final class StickerPickerSession {

    private final String id;
    private final long userId;
    private final long channelId;
    private final List<String> stickerIds;
    private final long expiresAt;
    private volatile int currentPage;

    public StickerPickerSession(String id, long userId, long channelId, List<String> stickerIds, long expiresAt) {
        this.id = id;
        this.userId = userId;
        this.channelId = channelId;
        this.stickerIds = List.copyOf(stickerIds);
        this.expiresAt = expiresAt;
    }

    public String id() {
        return id;
    }

    public long userId() {
        return userId;
    }

    public long channelId() {
        return channelId;
    }

    public List<String> stickerIds() {
        return stickerIds;
    }

    public int currentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, Math.min(currentPage, totalPages() - 1));
    }

    public int totalPages() {
        return Math.max(1, (stickerIds.size() + StickerPickerListener.PAGE_SIZE - 1) / StickerPickerListener.PAGE_SIZE);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
