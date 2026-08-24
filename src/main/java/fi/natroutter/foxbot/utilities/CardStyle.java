package fi.natroutter.foxbot.utilities;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The look the bot's rendered leaderboards share: dark rounded cards on a transparent background,
 * a coloured strip down the left edge for placing, and Discord's own greys so the images sit in a
 * channel as if they belonged there.
 *
 * <p>Holds only the vocabulary — palette, card, strip, text alignment — not any particular layout.
 */
public final class CardStyle {

    public static final int CARD = 0x313338;
    public static final int HEADER_PANEL = 0x1A1B1E;
    public static final int TABLE_HEADER = 0x26282C;
    public static final int TEXT = 0xF2F3F5;
    public static final int MUTED = 0xB5BAC1;
    public static final int LABEL = 0x8A9099;
    public static final int DIVIDER = 0x3F4147;

    /**
     * Rank accents. Everything below third place shares one neutral colour, which is deliberately
     * dim: silver has to read as a medal, and it cannot if the ranks under it are nearly as bright.
     */
    public static final int GOLD = 0xFFC93C;
    public static final int SILVER = 0xE4EAF2;
    public static final int BRONZE = 0xCD7F32;
    public static final int RANK_DEFAULT = 0x7B828D;

    /**
     * Podium cards, each shifted towards its own medal.
     *
     * <p>Second place has to be lifted as well as cooled: the card underneath it is already a cool
     * grey, so tinting alone leaves it looking like every other row. Gold and bronze need no such
     * help — a warm shift separates from this background on its own.
     */
    public static final int CARD_FIRST = 0x3A362C;
    public static final int CARD_SECOND = 0x3C434F;
    public static final int CARD_THIRD = 0x393430;

    public static final int CORNER = 10;
    public static final int ACCENT_BAR_WIDTH = 6;
    public static final int COLUMN_HEADER_HEIGHT = 32;

    private CardStyle() {
    }

    /** The accent for a placing: gold, silver, bronze, then one shared neutral. */
    public static Color rankColor(int rank) {
        return switch (rank) {
            case 1 -> new Color(GOLD);
            case 2 -> new Color(SILVER);
            case 3 -> new Color(BRONZE);
            default -> new Color(RANK_DEFAULT);
        };
    }

    /**
     * Filled rounded card with its placing strip, the base every row is drawn on top of.
     *
     * <p>The podium gets tinted fills: on a list of near-identical dark rows, the top three should
     * be recognisable before a single number has been read.
     */
    public static void card(Graphics2D graphics, int y, int width, int height, int rank) {
        graphics.setColor(new Color(switch (rank) {
            case 1 -> CARD_FIRST;
            case 2 -> CARD_SECOND;
            case 3 -> CARD_THIRD;
            default -> CARD;
        }));
        graphics.fillRoundRect(0, y, width, height, CORNER, CORNER);
        accentBar(graphics, y, height, rankColor(rank));
    }

    /**
     * Which size a placing is drawn at: 0 for the winner, 1 for the rest of the podium, 2 for
     * everyone else. Renderers index their own font and height tables with it, so the two
     * leaderboards step up and down together.
     */
    public static int tier(int rank) {
        if (rank == 1) {
            return 0;
        }
        return rank <= 3 ? 1 : 2;
    }

    /** Rounded bar down the left edge, squared off on its right so it reads as a strip. */
    public static void accentBar(Graphics2D graphics, int y, int height, Color accent) {
        graphics.setColor(accent);
        graphics.fillRoundRect(0, y, ACCENT_BAR_WIDTH * 2, height, CORNER, CORNER);
        graphics.fillRect(ACCENT_BAR_WIDTH, y, ACCENT_BAR_WIDTH, height);
    }

    /**
     * The strip above a card stack that names what each column holds, without which a row of bare
     * numbers is a guess.
     *
     * <p>Sets the label font and colour and returns the baseline to draw them on, so callers only
     * have to know their own column positions.
     */
    public static int columnHeader(Graphics2D graphics, int y, int width) {
        graphics.setColor(new Color(TABLE_HEADER));
        graphics.fillRoundRect(0, y, width, COLUMN_HEADER_HEIGHT, CORNER, CORNER);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        graphics.setColor(new Color(LABEL));
        return y + COLUMN_HEADER_HEIGHT / 2 + 5;
    }

    public static void rightAligned(Graphics2D graphics, String text, int rightX, int baseline) {
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, rightX - metrics.stringWidth(text), baseline);
    }

    public static BufferedImage transparentImage(int width, int height) {
        return new BufferedImage(width, Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
    }

    public static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    public static void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }
}
