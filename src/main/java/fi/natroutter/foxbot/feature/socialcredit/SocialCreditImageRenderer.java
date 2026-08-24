package fi.natroutter.foxbot.feature.socialcredit;

import fi.natroutter.foxbot.utilities.AvatarImageRenderer;
import fi.natroutter.foxbot.utilities.CardStyle;
import fi.natroutter.foxbot.utilities.EmojiTextRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Renders the social credit leaderboard as a stack of ranked cards, the same shape the voice
 * session leaderboard uses: placing strip, rank, avatar, name, and the number that earned the spot.
 *
 * <p>First place is drawn on a taller, warmer card at a larger size. A leaderboard where every row
 * is the same weight makes the reader work out who won; this way the top of the list announces it.
 */
public class SocialCreditImageRenderer {

    public static final int WIDTH = 960;

    // Indexed by CardStyle.tier: the winner, the rest of the podium, everyone else.
    private static final int[] CARD_HEIGHTS = {88, 76, 64};
    private static final int[] AVATAR_SIZES = {54, 44, 36};
    private static final int[] RANK_FONTS = {34, 30, 26};
    private static final int[] NAME_FONTS = {29, 26, 24};
    private static final int[] VALUE_FONTS = {27, 24, 22};
    private static final int[] BASELINE_NUDGE = {11, 10, 9};

    private static final int CARD_GAP = 8;

    private static final int RANK_X = 26;
    private static final int AVATAR_X = 88;
    private static final int NAME_GAP = 16;
    private static final int CREDITS_RIGHT = WIDTH - 28;
    /** Leaves room for a seven figure balance without the name ever running into it. */
    private static final int NAME_RESERVE = 200;

    // Display names carry emoji as often as channel names do.
    private static final EmojiTextRenderer EMOJI = new EmojiTextRenderer();
    private static final AvatarImageRenderer AVATARS = new AvatarImageRenderer();

    /** One place on the board. */
    public record Standing(int rank, String name, String avatarUrl, long credits) {
    }

    public byte[] render(List<Standing> standings) throws IOException {
        AVATARS.prefetch(standings.stream().map(Standing::avatarUrl).toList());

        BufferedImage image = CardStyle.transparentImage(WIDTH, height(standings));
        Graphics2D graphics = image.createGraphics();
        CardStyle.applyQualityHints(graphics);

        drawColumnHeader(graphics, 0);

        int y = CardStyle.COLUMN_HEADER_HEIGHT + CARD_GAP;
        for (Standing standing : standings) {
            drawCard(graphics, y, standing);
            y += cardHeight(standing.rank()) + CARD_GAP;
        }

        graphics.dispose();
        return CardStyle.toPng(image);
    }

    /** Names the columns, lined up with where the cards below draw each value. */
    private void drawColumnHeader(Graphics2D graphics, int y) {
        int baseline = CardStyle.columnHeader(graphics, y, WIDTH);
        graphics.drawString("#", RANK_X, baseline);
        // Over the avatar rather than the name: the picture is where the user column starts.
        graphics.drawString("USER", AVATAR_X, baseline);
        CardStyle.rightAligned(graphics, "SOCIAL CREDITS", CREDITS_RIGHT, baseline);
    }

    private static int height(List<Standing> standings) {
        int height = CardStyle.COLUMN_HEADER_HEIGHT;
        for (Standing standing : standings) {
            height += CARD_GAP + cardHeight(standing.rank());
        }
        return height;
    }

    private static int cardHeight(int rank) {
        return CARD_HEIGHTS[CardStyle.tier(rank)];
    }

    private void drawCard(Graphics2D graphics, int y, Standing standing) {
        int tier = CardStyle.tier(standing.rank());
        int height = CARD_HEIGHTS[tier];
        int avatarSize = AVATAR_SIZES[tier];
        int nameX = AVATAR_X + avatarSize + NAME_GAP;

        CardStyle.card(graphics, y, WIDTH, height, standing.rank());

        int baseline = y + height / 2 + BASELINE_NUDGE[tier];

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, RANK_FONTS[tier]));
        graphics.setColor(CardStyle.rankColor(standing.rank()));
        graphics.drawString("#" + standing.rank(), RANK_X, baseline);

        AVATARS.draw(graphics, standing.avatarUrl(), standing.name(),
                AVATAR_X, y + (height - avatarSize) / 2, avatarSize);

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, NAME_FONTS[tier]));
        graphics.setColor(new Color(CardStyle.TEXT));
        EMOJI.drawTruncated(graphics, standing.name(), nameX, baseline,
                CREDITS_RIGHT - nameX - NAME_RESERVE);

        // Grouped, because six and seven figure balances are unreadable as a run of digits.
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, VALUE_FONTS[tier]));
        CardStyle.rightAligned(graphics, format(standing.credits()), CREDITS_RIGHT, baseline);
    }

    private static String format(long credits) {
        return NumberFormat.getIntegerInstance(Locale.US).format(credits);
    }
}
