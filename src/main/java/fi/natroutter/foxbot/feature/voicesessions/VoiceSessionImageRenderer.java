package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.utilities.AvatarImageRenderer;
import fi.natroutter.foxbot.utilities.EmojiTextRenderer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Renders voice session images on a transparent background.
 *
 * <p>Three layouts, all built from the same card vocabulary:
 * <ul>
 *   <li>{@code renderTop} — a stack of ranked one-line cards.</li>
 *   <li>{@code renderDetail} — one session as a summary panel plus its participant table.</li>
 *   <li>{@code renderCurrent} — the same block as detail, repeated per running session.</li>
 * </ul>
 */
public class VoiceSessionImageRenderer {

    public static final int WIDTH = 960;
    public static final int TOP_PAGE_SIZE = 10;
    /** Each block carries a whole participant table, so far fewer fit comfortably in one image. */
    public static final int CURRENT_PAGE_SIZE = 3;

    private static final int CARD_HEIGHT = 64;
    private static final int CARD_GAP = 8;
    private static final int ACCENT_BAR_WIDTH = 6;
    private static final int CORNER = 10;

    private static final int CARD = 0x313338;
    private static final int HEADER_PANEL = 0x1A1B1E;
    private static final int TABLE_HEADER = 0x26282C;
    private static final int TEXT = 0xF2F3F5;
    private static final int MUTED = 0xB5BAC1;
    private static final int LABEL = 0x8A9099;
    private static final int DIVIDER = 0x3F4147;
    private static final int LIVE = 0x3BA55D;

    /** Rank accents. Everything below third place shares one neutral colour. */
    private static final int GOLD = 0xFFC93C;
    private static final int SILVER = 0xC5CBD3;
    private static final int BRONZE = 0xCD7F32;
    private static final int RANK_DEFAULT = 0x9BA3AE;

    // Block geometry, shared by the detail and current layouts.
    private static final int HEADER_HEIGHT = 136;
    private static final int HEADER_TO_TABLE_GAP = 18;
    private static final int COLUMN_HEADER_HEIGHT = 32;
    private static final int PARTICIPANT_ROW_HEIGHT = 42;
    private static final int PARTICIPANT_GAP = 4;
    private static final int BLOCK_GAP = 22;
    private static final int MAX_PARTICIPANTS_DETAIL = 15;
    private static final int MAX_PARTICIPANTS_CURRENT = 10;

    // Shared column positions, so the table header lines up with the rows beneath it.
    private static final int COL_RANK_X = 28;
    private static final int COL_AVATAR_X = 58;
    private static final int AVATAR_SIZE = 28;
    private static final int COL_USER_X = COL_AVATAR_X + AVATAR_SIZE + 12;
    private static final int COL_TIME_RIGHT = WIDTH - 28;

    // Channel and user names routinely contain emoji, which Java2D cannot draw from a colour font.
    private static final EmojiTextRenderer EMOJI = new EmojiTextRenderer();

    // Participant rows carry the user's Discord picture, so the reader recognises the list at a glance.
    private static final AvatarImageRenderer AVATARS = new AvatarImageRenderer();

    private static final int CARD_NAME_X = 112;
    private static final int CARD_NAME_MAX_WIDTH = 470 - CARD_NAME_X - 16;
    private static final int HEADER_NAME_MAX_WIDTH = WIDTH - 28 - 160;
    /** The reserve on the right has to fit a full "1d 2h 5m 12s", not just a single unit. */
    private static final int PARTICIPANT_NAME_MAX_WIDTH = COL_TIME_RIGHT - COL_USER_X - 190;

    // The payout card is a three-place podium, so its rows are roomier than a participant table's
    // and carry a second value column for the credits.
    private static final int PAYOUT_ROW_HEIGHT = 48;
    private static final int PAYOUT_AVATAR_SIZE = 32;
    private static final int COL_PAYOUT_USER_X = COL_AVATAR_X + PAYOUT_AVATAR_SIZE + 12;
    private static final int COL_PAYOUT_TIME_RIGHT = WIDTH - 150;
    private static final int COL_CREDITS_RIGHT = COL_TIME_RIGHT;
    private static final int PAYOUT_NAME_MAX_WIDTH = COL_PAYOUT_TIME_RIGHT - COL_PAYOUT_USER_X - 190;

    public byte[] renderTop(List<VoiceSessionEntry> sessions, int page, int totalPages) throws IOException {
        int count = Math.max(1, sessions.size());
        BufferedImage image = transparentImage(WIDTH, count * CARD_HEIGHT + (count - 1) * CARD_GAP);
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        for (int index = 0; index < sessions.size(); index++) {
            int y = index * (CARD_HEIGHT + CARD_GAP);
            drawLeaderboardCard(graphics, y, page * TOP_PAGE_SIZE + index + 1, sessions.get(index));
        }

        graphics.dispose();
        return toPng(image);
    }

    public byte[] renderDetail(VoiceSessionEntry session, int rank) throws IOException {
        List<VoiceSessionEntry.VoiceParticipant> participants = topParticipants(session, MAX_PARTICIPANTS_DETAIL);
        prefetchAvatars(List.of(participants));

        BufferedImage image = transparentImage(WIDTH, blockHeight(participants.size()));
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        drawBlock(graphics, 0, session, rank, participants);

        graphics.dispose();
        return toPng(image);
    }

    /** One detail-style block per running session, stacked. */
    public byte[] renderCurrent(List<VoiceSessionEntry> sessions) throws IOException {
        List<List<VoiceSessionEntry.VoiceParticipant>> blocks = sessions.stream()
                .map(session -> topParticipants(session, MAX_PARTICIPANTS_CURRENT))
                .toList();
        prefetchAvatars(blocks);

        int height = 0;
        for (List<VoiceSessionEntry.VoiceParticipant> participants : blocks) {
            height += blockHeight(participants.size()) + BLOCK_GAP;
        }
        height = Math.max(1, height - BLOCK_GAP);

        BufferedImage image = transparentImage(WIDTH, height);
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        int y = 0;
        for (int index = 0; index < sessions.size(); index++) {
            List<VoiceSessionEntry.VoiceParticipant> participants = blocks.get(index);
            drawBlock(graphics, y, sessions.get(index), 0, participants);
            y += blockHeight(participants.size()) + BLOCK_GAP;
        }

        graphics.dispose();
        return toPng(image);
    }

    /**
     * The podium of a finished session and what it paid, posted into the channel's own chat when
     * the session ends.
     */
    public byte[] renderPayout(VoiceSessionEntry session, List<VoiceSessionRewards.Payout> payouts) throws IOException {
        AVATARS.prefetch(payouts.stream().map(payout -> avatarUrl(payout.participant())).toList());

        int rows = Math.max(1, payouts.size());
        int height = HEADER_HEIGHT + HEADER_TO_TABLE_GAP
                + COLUMN_HEADER_HEIGHT + PARTICIPANT_GAP
                + rows * PAYOUT_ROW_HEIGHT + (rows - 1) * PARTICIPANT_GAP;

        BufferedImage image = transparentImage(WIDTH, height);
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        drawPayoutHeader(graphics, 0, session);

        int tableTop = HEADER_HEIGHT + HEADER_TO_TABLE_GAP;
        drawPayoutColumnHeader(graphics, tableTop);

        int rowsTop = tableTop + COLUMN_HEADER_HEIGHT + PARTICIPANT_GAP;
        for (int index = 0; index < payouts.size(); index++) {
            drawPayoutRow(graphics, rowsTop + index * (PAYOUT_ROW_HEIGHT + PARTICIPANT_GAP), payouts.get(index));
        }

        graphics.dispose();
        return toPng(image);
    }

    /**
     * Warms every avatar the image will need in one parallel pass, so a cold cache costs one round
     * trip for the whole render instead of one per row.
     */
    private static void prefetchAvatars(List<List<VoiceSessionEntry.VoiceParticipant>> blocks) {
        AVATARS.prefetch(blocks.stream()
                .flatMap(List::stream)
                .map(VoiceSessionImageRenderer::avatarUrl)
                .toList());
    }

    /** Falls back to the Discord default avatar for sessions recorded before avatars were stored. */
    private static String avatarUrl(VoiceSessionEntry.VoiceParticipant participant) {
        String url = participant.getAvatarUrl();
        return url == null || url.isBlank()
                ? AvatarImageRenderer.defaultAvatarUrl(participant.getUserID())
                : url;
    }

    /**
     * Every non-zero unit, largest first — {@code "1d 2h 5m 12s"}. Units that are zero are left
     * out entirely rather than padded, so a duration never hides the part that is still moving.
     */
    public static String formatDuration(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder text = new StringBuilder();
        appendUnit(text, days, "d");
        appendUnit(text, hours, "h");
        appendUnit(text, minutes, "m");
        appendUnit(text, seconds, "s");
        return text.isEmpty() ? "0s" : text.toString();
    }

    private static void appendUnit(StringBuilder text, long value, String unit) {
        if (value == 0) {
            return;
        }
        if (!text.isEmpty()) {
            text.append(' ');
        }
        text.append(value).append(unit);
    }

    /** The accent for a placing: gold, silver, bronze, then one shared neutral. */
    private static Color rankColor(int rank) {
        return switch (rank) {
            case 1 -> new Color(GOLD);
            case 2 -> new Color(SILVER);
            case 3 -> new Color(BRONZE);
            default -> new Color(RANK_DEFAULT);
        };
    }

    private static List<VoiceSessionEntry.VoiceParticipant> topParticipants(VoiceSessionEntry session, int limit) {
        if (session.getParticipants() == null) {
            return List.of();
        }
        return session.getParticipants().stream()
                .sorted(Comparator.comparingLong(VoiceSessionEntry.VoiceParticipant::getTotalSeconds).reversed())
                .limit(limit)
                .toList();
    }

    private static int blockHeight(int participantCount) {
        int rows = Math.max(1, participantCount);
        return HEADER_HEIGHT
                + HEADER_TO_TABLE_GAP
                + COLUMN_HEADER_HEIGHT + PARTICIPANT_GAP
                + rows * PARTICIPANT_ROW_HEIGHT + (rows - 1) * PARTICIPANT_GAP;
    }

    private void drawLeaderboardCard(Graphics2D graphics, int y, int rank, VoiceSessionEntry session) {
        Color accent = rankColor(rank);

        graphics.setColor(new Color(CARD));
        graphics.fillRoundRect(0, y, WIDTH, CARD_HEIGHT, CORNER, CORNER);
        drawAccentBar(graphics, y, CARD_HEIGHT, accent);

        int baseline = y + CARD_HEIGHT / 2 + 9;

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        graphics.setColor(accent);
        graphics.drawString("#" + rank, 26, baseline);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.setColor(new Color(TEXT));
        EMOJI.drawTruncated(graphics, session.getChannelName(), CARD_NAME_X, baseline, CARD_NAME_MAX_WIDTH);

        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        graphics.drawString(formatDuration(session.getDurationSeconds()), 470, baseline);

        int userCount = session.getParticipants().size();
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        graphics.setColor(new Color(MUTED));
        drawRightAligned(graphics, userCount + (userCount == 1 ? " user" : " users"), WIDTH - 210, baseline);

        // An active session has no real end time yet — its endedAt is only the last checkpoint.
        if (session.isActive()) {
            graphics.setColor(new Color(LIVE));
            drawRightAligned(graphics, "● Live", WIDTH - 24, baseline);
        } else {
            drawRightAligned(graphics, formatTime(session.getEndedAt()), WIDTH - 24, baseline);
        }
    }

    /**
     * Summary panel plus participant table for one session.
     *
     * @param rank placing to show top-right; ignored for a running session, which shows a LIVE
     *             badge instead because "how long has it been going" matters more than a placing
     *             that is still moving.
     */
    private void drawBlock(Graphics2D graphics, int y, VoiceSessionEntry session, int rank,
                           List<VoiceSessionEntry.VoiceParticipant> participants) {
        drawBlockHeader(graphics, y, session, rank);

        int tableTop = y + HEADER_HEIGHT + HEADER_TO_TABLE_GAP;
        drawColumnHeader(graphics, tableTop);

        int rowsTop = tableTop + COLUMN_HEADER_HEIGHT + PARTICIPANT_GAP;
        for (int index = 0; index < participants.size(); index++) {
            drawParticipantRow(graphics, rowsTop + index * (PARTICIPANT_ROW_HEIGHT + PARTICIPANT_GAP),
                    index + 1, participants.get(index));
        }
    }

    private void drawBlockHeader(Graphics2D graphics, int y, VoiceSessionEntry session, int rank) {
        boolean live = session.isActive();
        Color accent = live ? new Color(LIVE) : rankColor(rank);

        graphics.setColor(new Color(HEADER_PANEL));
        graphics.fillRoundRect(0, y, WIDTH, HEADER_HEIGHT, CORNER, CORNER);
        drawAccentBar(graphics, y, HEADER_HEIGHT, accent);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.setColor(new Color(TEXT));
        EMOJI.drawTruncated(graphics, session.getChannelName(), 28, y + 46, HEADER_NAME_MAX_WIDTH);

        if (live) {
            drawLiveBadge(graphics, WIDTH - 28, y + 46);
        } else {
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            graphics.setColor(accent);
            drawRightAligned(graphics, "#" + rank, WIDTH - 28, y + 46);
        }

        graphics.setColor(new Color(DIVIDER));
        graphics.fillRect(28, y + 62, WIDTH - 56, 1);

        Color valueColor = live ? new Color(LIVE) : new Color(TEXT);
        drawStat(graphics, 28, y, live ? "DURATION SO FAR" : "DURATION",
                formatDuration(session.getDurationSeconds()), valueColor);
        drawStat(graphics, 300, y, "PARTICIPANTS",
                String.valueOf(session.getParticipants() == null ? 0 : session.getParticipants().size()), new Color(TEXT));
        drawStat(graphics, 560, y, live ? "STARTED" : "ENDED",
                formatTime(live ? session.getStartedAt() : session.getEndedAt()), new Color(TEXT));
    }

    /**
     * Same panel as a session block, but the third stat explains the payout: credits are multiplied
     * by the whole hours the session ran, so the multiplier is the number that answers "why this
     * much?".
     */
    private void drawPayoutHeader(Graphics2D graphics, int y, VoiceSessionEntry session) {
        Color accent = new Color(GOLD);

        graphics.setColor(new Color(HEADER_PANEL));
        graphics.fillRoundRect(0, y, WIDTH, HEADER_HEIGHT, CORNER, CORNER);
        drawAccentBar(graphics, y, HEADER_HEIGHT, accent);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.setColor(new Color(TEXT));
        EMOJI.drawTruncated(graphics, session.getChannelName(), 28, y + 46, HEADER_NAME_MAX_WIDTH);

        drawBadge(graphics, WIDTH - 28, y + 46, "REWARDS", accent);

        graphics.setColor(new Color(DIVIDER));
        graphics.fillRect(28, y + 62, WIDTH - 56, 1);

        int participants = session.getParticipants() == null ? 0 : session.getParticipants().size();
        drawStat(graphics, 28, y, "SESSION LENGTH", formatDuration(session.getDurationSeconds()), new Color(TEXT));
        drawStat(graphics, 300, y, "PARTICIPANTS", String.valueOf(participants), new Color(TEXT));
        drawStat(graphics, 560, y, "REWARD MULTIPLIER",
                "x" + VoiceSessionRewards.multiplier(session.getDurationSeconds()), accent);
    }

    private void drawPayoutColumnHeader(Graphics2D graphics, int y) {
        graphics.setColor(new Color(TABLE_HEADER));
        graphics.fillRoundRect(0, y, WIDTH, COLUMN_HEADER_HEIGHT, CORNER, CORNER);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        graphics.setColor(new Color(LABEL));

        int baseline = y + COLUMN_HEADER_HEIGHT / 2 + 5;
        graphics.drawString("#", COL_RANK_X, baseline);
        graphics.drawString("USER", COL_AVATAR_X, baseline);
        drawRightAligned(graphics, "TIME IN CHANNEL", COL_PAYOUT_TIME_RIGHT, baseline);
        drawRightAligned(graphics, "CREDITS", COL_CREDITS_RIGHT, baseline);
    }

    private void drawPayoutRow(Graphics2D graphics, int y, VoiceSessionRewards.Payout payout) {
        Color accent = rankColor(payout.place());
        VoiceSessionEntry.VoiceParticipant participant = payout.participant();

        graphics.setColor(new Color(CARD));
        graphics.fillRoundRect(0, y, WIDTH, PAYOUT_ROW_HEIGHT, CORNER, CORNER);
        drawAccentBar(graphics, y, PAYOUT_ROW_HEIGHT, accent);

        int baseline = y + PAYOUT_ROW_HEIGHT / 2 + 7;

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.setColor(accent);
        graphics.drawString("#" + payout.place(), COL_RANK_X, baseline);

        AVATARS.draw(graphics, avatarUrl(participant), participant.getUsername(),
                COL_AVATAR_X, y + (PAYOUT_ROW_HEIGHT - PAYOUT_AVATAR_SIZE) / 2, PAYOUT_AVATAR_SIZE);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        graphics.setColor(new Color(TEXT));
        EMOJI.drawTruncated(graphics, participant.getUsername(), COL_PAYOUT_USER_X, baseline, PAYOUT_NAME_MAX_WIDTH);

        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        graphics.setColor(new Color(MUTED));
        drawRightAligned(graphics, formatDuration(participant.getTotalSeconds()), COL_PAYOUT_TIME_RIGHT, baseline);

        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        graphics.setColor(accent);
        drawRightAligned(graphics, "+" + payout.credits(), COL_CREDITS_RIGHT, baseline);
    }

    /** Filled pill with dark text, for a label that should read as a stamp rather than a value. */
    private void drawBadge(Graphics2D graphics, int rightX, int baseline, String text, Color color) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        FontMetrics metrics = graphics.getFontMetrics();

        int paddingX = 14;
        int badgeWidth = metrics.stringWidth(text) + paddingX * 2;
        int badgeHeight = 28;
        int left = rightX - badgeWidth;
        int top = baseline - 20;

        graphics.setColor(color);
        graphics.fillRoundRect(left, top, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

        graphics.setColor(new Color(HEADER_PANEL));
        graphics.drawString(text, left + paddingX, top + badgeHeight / 2 + 5);
    }

    /** Green pill in place of the rank, so a running session is unmistakable at a glance. */
    private void drawLiveBadge(Graphics2D graphics, int rightX, int baseline) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        FontMetrics metrics = graphics.getFontMetrics();

        String text = "LIVE";
        int paddingX = 14;
        int badgeWidth = metrics.stringWidth(text) + paddingX * 2 + 14;
        int badgeHeight = 28;
        int left = rightX - badgeWidth;
        int top = baseline - 20;

        graphics.setColor(new Color(LIVE));
        graphics.fillRoundRect(left, top, badgeWidth, badgeHeight, badgeHeight, badgeHeight);

        int dotSize = 8;
        graphics.setColor(new Color(0xFFFFFF));
        graphics.fillOval(left + paddingX - 2, top + (badgeHeight - dotSize) / 2, dotSize, dotSize);
        graphics.drawString(text, left + paddingX + dotSize + 4, top + badgeHeight / 2 + 5);
    }

    private void drawStat(Graphics2D graphics, int x, int y, String label, String value, Color valueColor) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        graphics.setColor(new Color(LABEL));
        graphics.drawString(label, x, y + 84);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        graphics.setColor(valueColor);
        graphics.drawString(value, x, y + 108);
    }

    /** Names the columns of the participant table so the rank and time values are self-explanatory. */
    private void drawColumnHeader(Graphics2D graphics, int y) {
        graphics.setColor(new Color(TABLE_HEADER));
        graphics.fillRoundRect(0, y, WIDTH, COLUMN_HEADER_HEIGHT, CORNER, CORNER);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        graphics.setColor(new Color(LABEL));

        int baseline = y + COLUMN_HEADER_HEIGHT / 2 + 5;
        graphics.drawString("#", COL_RANK_X, baseline);
        // Over the avatar rather than the name: the picture is where the user column now starts.
        graphics.drawString("USER", COL_AVATAR_X, baseline);
        drawRightAligned(graphics, "TIME IN CHANNEL", COL_TIME_RIGHT, baseline);
    }

    private void drawParticipantRow(Graphics2D graphics, int y, int rank, VoiceSessionEntry.VoiceParticipant participant) {
        Color accent = rankColor(rank);

        graphics.setColor(new Color(CARD));
        graphics.fillRoundRect(0, y, WIDTH, PARTICIPANT_ROW_HEIGHT, CORNER, CORNER);
        drawAccentBar(graphics, y, PARTICIPANT_ROW_HEIGHT, accent);

        int baseline = y + PARTICIPANT_ROW_HEIGHT / 2 + 6;

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        graphics.setColor(accent);
        graphics.drawString(String.valueOf(rank), COL_RANK_X, baseline);

        AVATARS.draw(graphics, avatarUrl(participant), participant.getUsername(),
                COL_AVATAR_X, y + (PARTICIPANT_ROW_HEIGHT - AVATAR_SIZE) / 2, AVATAR_SIZE);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        graphics.setColor(new Color(TEXT));
        EMOJI.drawTruncated(graphics, participant.getUsername(), COL_USER_X, baseline, PARTICIPANT_NAME_MAX_WIDTH);

        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        drawRightAligned(graphics, formatDuration(participant.getTotalSeconds()), COL_TIME_RIGHT, baseline);
    }

    /** Rounded bar down the left edge, squared off on its right so it reads as a strip. */
    private void drawAccentBar(Graphics2D graphics, int y, int height, Color accent) {
        graphics.setColor(accent);
        graphics.fillRoundRect(0, y, ACCENT_BAR_WIDTH * 2, height, CORNER, CORNER);
        graphics.fillRect(ACCENT_BAR_WIDTH, y, ACCENT_BAR_WIDTH, height);
    }

    private void drawRightAligned(Graphics2D graphics, String text, int rightX, int baseline) {
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, rightX - metrics.stringWidth(text), baseline);
    }

    private BufferedImage transparentImage(int width, int height) {
        return new BufferedImage(width, Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    }

    private byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    private static String formatTime(long epochMillis) {
        var time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDate date = time.toLocalDate();
        LocalDate today = LocalDate.now();
        String clock = time.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (date.equals(today)) {
            return "Today " + clock;
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday " + clock;
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
