package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
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
    private static final int PARTICIPANT_ROW_HEIGHT = 34;
    private static final int PARTICIPANT_GAP = 4;
    private static final int BLOCK_GAP = 22;
    private static final int MAX_PARTICIPANTS_DETAIL = 15;
    private static final int MAX_PARTICIPANTS_CURRENT = 10;

    // Shared column positions, so the table header lines up with the rows beneath it.
    private static final int COL_RANK_X = 30;
    private static final int COL_USER_X = 84;
    private static final int COL_TIME_RIGHT = WIDTH - 28;

    // Channel and user names routinely contain emoji, which Java2D cannot draw from a colour font.
    private static final EmojiTextRenderer EMOJI = new EmojiTextRenderer();

    private static final int CARD_NAME_X = 112;
    private static final int CARD_NAME_MAX_WIDTH = 470 - CARD_NAME_X - 16;
    private static final int HEADER_NAME_MAX_WIDTH = WIDTH - 28 - 160;
    private static final int PARTICIPANT_NAME_MAX_WIDTH = COL_TIME_RIGHT - COL_USER_X - 120;

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

        BufferedImage image = transparentImage(WIDTH, blockHeight(participants.size()));
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        drawBlock(graphics, 0, session, rank, participants);

        graphics.dispose();
        return toPng(image);
    }

    /** One detail-style block per running session, stacked. */
    public byte[] renderCurrent(List<VoiceSessionEntry> sessions) throws IOException {
        int height = 0;
        for (VoiceSessionEntry session : sessions) {
            height += blockHeight(topParticipants(session, MAX_PARTICIPANTS_CURRENT).size()) + BLOCK_GAP;
        }
        height = Math.max(1, height - BLOCK_GAP);

        BufferedImage image = transparentImage(WIDTH, height);
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        int y = 0;
        for (VoiceSessionEntry session : sessions) {
            List<VoiceSessionEntry.VoiceParticipant> participants = topParticipants(session, MAX_PARTICIPANTS_CURRENT);
            drawBlock(graphics, y, session, 0, participants);
            y += blockHeight(participants.size()) + BLOCK_GAP;
        }

        graphics.dispose();
        return toPng(image);
    }

    public static String formatDuration(long totalSeconds) {
        long seconds = Math.max(0, totalSeconds);
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return seconds + "s";
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
        graphics.drawString("USER", COL_USER_X, baseline);
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
