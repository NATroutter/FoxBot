package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.utilities.Utils;

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
 * <p>The leaderboard is a stack of ranked cards and nothing else — no title, no page counter, no
 * outer padding, since the embed and buttons already carry that. The detail view is deliberately
 * built from different parts (a dark summary panel and a labelled table) so it cannot be mistaken
 * for the leaderboard at a glance.
 */
public class VoiceSessionImageRenderer {

    public static final int WIDTH = 960;
    public static final int TOP_PAGE_SIZE = 10;

    private static final int CARD_HEIGHT = 64;
    private static final int CARD_GAP = 8;
    private static final int ACCENT_BAR_WIDTH = 6;
    private static final int CORNER = 10;

    private static final int CARD = 0x313338;
    private static final int HEADER_PANEL = 0x1A1B1E;
    /** Between the info panel and the rows, so the table header is opaque rather than a see-through band. */
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

    // Detail view geometry. The stat values sit on baseline 108, so the panel runs well past that
    // to leave real breathing room underneath rather than clipping close to the descenders.
    private static final int HEADER_HEIGHT = 136;
    private static final int HEADER_TO_TABLE_GAP = 18;
    private static final int COLUMN_HEADER_HEIGHT = 32;
    private static final int PARTICIPANT_ROW_HEIGHT = 34;
    private static final int PARTICIPANT_GAP = 4;
    private static final int MAX_PARTICIPANTS = 15;

    // Shared column positions, so the table header lines up with the rows beneath it.
    private static final int COL_RANK_X = 30;
    private static final int COL_USER_X = 84;
    private static final int COL_TIME_RIGHT = WIDTH - 28;

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
        List<VoiceSessionEntry.VoiceParticipant> participants = session.getParticipants().stream()
                .sorted(Comparator.comparingLong(VoiceSessionEntry.VoiceParticipant::getTotalSeconds).reversed())
                .limit(MAX_PARTICIPANTS)
                .toList();

        int rows = Math.max(1, participants.size());
        int height = HEADER_HEIGHT
                + HEADER_TO_TABLE_GAP
                + COLUMN_HEADER_HEIGHT + PARTICIPANT_GAP
                + rows * PARTICIPANT_ROW_HEIGHT + (rows - 1) * PARTICIPANT_GAP;

        BufferedImage image = transparentImage(WIDTH, height);
        Graphics2D graphics = image.createGraphics();
        applyQualityHints(graphics);

        drawDetailHeader(graphics, session, rank);

        int tableTop = HEADER_HEIGHT + HEADER_TO_TABLE_GAP;
        drawColumnHeader(graphics, tableTop);

        int rowsTop = tableTop + COLUMN_HEADER_HEIGHT + PARTICIPANT_GAP;
        for (int index = 0; index < participants.size(); index++) {
            drawParticipantRow(graphics, rowsTop + index * (PARTICIPANT_ROW_HEIGHT + PARTICIPANT_GAP),
                    index + 1, participants.get(index));
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

    private void drawLeaderboardCard(Graphics2D graphics, int y, int rank, VoiceSessionEntry session) {
        Color accent = rankColor(rank);

        graphics.setColor(new Color(CARD));
        graphics.fillRoundRect(0, y, WIDTH, CARD_HEIGHT, CORNER, CORNER);
        graphics.setColor(accent);
        graphics.fillRoundRect(0, y, ACCENT_BAR_WIDTH * 2, CARD_HEIGHT, CORNER, CORNER);
        graphics.fillRect(ACCENT_BAR_WIDTH, y, ACCENT_BAR_WIDTH, CARD_HEIGHT);

        int baseline = y + CARD_HEIGHT / 2 + 9;

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        graphics.setColor(accent);
        graphics.drawString("#" + rank, 26, baseline);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.setColor(new Color(TEXT));
        graphics.drawString(Utils.cutStringEndDots(session.getChannelName(), 20), 112, baseline);

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
     * Dark summary panel: the channel name, then each statistic under its own label so the numbers
     * are not left to be guessed at. No rank stripe here — that belongs to the leaderboard.
     */
    private void drawDetailHeader(Graphics2D graphics, VoiceSessionEntry session, int rank) {
        graphics.setColor(new Color(HEADER_PANEL));
        graphics.fillRoundRect(0, 0, WIDTH, HEADER_HEIGHT, CORNER, CORNER);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        graphics.setColor(new Color(TEXT));
        graphics.drawString(Utils.cutStringEndDots(session.getChannelName(), 34), 28, 46);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        graphics.setColor(rankColor(rank));
        drawRightAligned(graphics, "#" + rank, WIDTH - 28, 46);

        graphics.setColor(new Color(DIVIDER));
        graphics.fillRect(28, 62, WIDTH - 56, 1);

        boolean live = session.isActive();
        Color liveColor = live ? new Color(LIVE) : new Color(TEXT);

        drawStat(graphics, 28, live ? "DURATION SO FAR" : "DURATION",
                formatDuration(session.getDurationSeconds()), liveColor);
        drawStat(graphics, 300, "PARTICIPANTS", String.valueOf(session.getParticipants().size()), new Color(TEXT));
        drawStat(graphics, 560, live ? "STATUS" : "ENDED",
                live ? "In progress" : formatTime(session.getEndedAt()), liveColor);
    }

    private void drawStat(Graphics2D graphics, int x, String label, String value, Color valueColor) {
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        graphics.setColor(new Color(LABEL));
        graphics.drawString(label, x, 84);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        graphics.setColor(valueColor);
        graphics.drawString(value, x, 108);
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
        graphics.setColor(new Color(CARD));
        graphics.fillRoundRect(0, y, WIDTH, PARTICIPANT_ROW_HEIGHT, CORNER, CORNER);

        int baseline = y + PARTICIPANT_ROW_HEIGHT / 2 + 6;

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        graphics.setColor(rankColor(rank));
        graphics.drawString(String.valueOf(rank), COL_RANK_X, baseline);

        graphics.setColor(new Color(TEXT));
        graphics.drawString(Utils.cutStringEndDots(participant.getUsername(), 40), COL_USER_X, baseline);

        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 17));
        drawRightAligned(graphics, formatDuration(participant.getTotalSeconds()), COL_TIME_RIGHT, baseline);
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
