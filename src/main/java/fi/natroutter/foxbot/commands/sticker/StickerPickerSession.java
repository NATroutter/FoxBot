package fi.natroutter.foxbot.commands.sticker;

import java.util.List;

final class StickerPickerSession {

    private final String id;
    private final long userId;
    private final long channelId;
    private final List<String> stickerIds;
    private final long expiresAt;
    private volatile int currentPage;

    StickerPickerSession(String id, long userId, long channelId, List<String> stickerIds, long expiresAt) {
        this.id = id;
        this.userId = userId;
        this.channelId = channelId;
        this.stickerIds = List.copyOf(stickerIds);
        this.expiresAt = expiresAt;
    }

    String id() {
        return id;
    }

    long userId() {
        return userId;
    }

    long channelId() {
        return channelId;
    }

    List<String> stickerIds() {
        return stickerIds;
    }

    int currentPage() {
        return currentPage;
    }

    void setCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, Math.min(currentPage, totalPages() - 1));
    }

    int totalPages() {
        return Math.max(1, (stickerIds.size() + StickerPickerListener.PAGE_SIZE - 1) / StickerPickerListener.PAGE_SIZE);
    }

    boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}
