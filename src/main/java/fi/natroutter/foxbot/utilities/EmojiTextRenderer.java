package fi.natroutter.foxbot.utilities;

import javax.imageio.ImageIO;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Draws text that may contain emoji into a {@link Graphics2D}.
 *
 * <p>Java2D cannot rasterise colour emoji fonts (COLR/CPAL), so drawing a channel name straight
 * through {@code drawString} produces flat monochrome silhouettes where an emoji font exists, and
 * blank boxes where one does not — which is most headless Linux servers. Instead each emoji is
 * drawn as a Twemoji image, giving identical colour output on every host and matching how Discord
 * itself renders the same name.
 *
 * <p>Images are fetched once per unique emoji and cached on disk, so a given emoji costs one
 * request ever and nothing at all after the first run. If an emoji cannot be fetched — no network,
 * or a sequence Twemoji does not ship — it falls back to drawing the character with the current
 * font, which is no worse than the previous behaviour.
 */
public final class EmojiTextRenderer {

    private static final String CDN = "https://cdn.jsdelivr.net/gh/jdecked/twemoji@15.1.0/assets/72x72/";
    private static final String CACHE_DIRECTORY = "emoji-cache";
    private static final String ELLIPSIS = "...";

    private static final int ZWJ = 0x200D;
    private static final int VARIATION_SELECTOR_16 = 0xFE0F;
    private static final int VARIATION_SELECTOR_15 = 0xFE0E;
    private static final int KEYCAP = 0x20E3;

    /** Absent value means "known to be unavailable", so a miss is not retried on every render. */
    private final ConcurrentMap<String, Optional<BufferedImage>> images = new ConcurrentHashMap<>();

    /**
     * Deliberately short. A fetch happens on the render thread, so an unreachable CDN must fail
     * fast rather than stall the command; a failure is cached so it costs at most one attempt per
     * emoji per run.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final File cacheDirectory;

    public EmojiTextRenderer() {
        File directory = new File(CACHE_DIRECTORY);
        this.cacheDirectory = directory.isDirectory() || directory.mkdirs() ? directory : null;
    }

    /** Pixel width of {@code text} as it would be drawn with the graphics context's current font. */
    public int measure(Graphics2D graphics, String text) {
        FontMetrics metrics = graphics.getFontMetrics();
        int emojiSize = emojiSize(graphics);
        int width = 0;
        for (Token token : tokenize(text)) {
            width += token.emoji() ? emojiAdvance(emojiSize) : metrics.stringWidth(token.value());
        }
        return width;
    }

    public void draw(Graphics2D graphics, String text, int x, int baseline) {
        FontMetrics metrics = graphics.getFontMetrics();
        int emojiSize = emojiSize(graphics);
        int top = emojiTop(graphics, baseline, emojiSize);
        int cursor = x;

        for (Token token : tokenize(text)) {
            if (!token.emoji()) {
                graphics.drawString(token.value(), cursor, baseline);
                cursor += metrics.stringWidth(token.value());
                continue;
            }

            BufferedImage image = image(token.value()).orElse(null);
            if (image == null) {
                // No Twemoji asset: fall back to the font, which at worst matches the old output.
                graphics.drawString(token.value(), cursor, baseline);
                cursor += metrics.stringWidth(token.value());
                continue;
            }

            graphics.drawImage(image, cursor, top, emojiSize, emojiSize, null);
            cursor += emojiAdvance(emojiSize);
        }
    }

    /**
     * Draws {@code text}, trimming it to fit {@code maxWidth} and appending an ellipsis when it
     * does not. Trimming happens on token boundaries, so an emoji is never cut in half.
     */
    public void drawTruncated(Graphics2D graphics, String text, int x, int baseline, int maxWidth) {
        if (measure(graphics, text) <= maxWidth) {
            draw(graphics, text, x, baseline);
            return;
        }

        FontMetrics metrics = graphics.getFontMetrics();
        int emojiSize = emojiSize(graphics);
        int budget = maxWidth - metrics.stringWidth(ELLIPSIS);

        StringBuilder kept = new StringBuilder();
        int width = 0;
        for (Token token : tokenize(text)) {
            int tokenWidth = token.emoji() ? emojiAdvance(emojiSize) : metrics.stringWidth(token.value());
            if (token.emoji()) {
                if (width + tokenWidth > budget) {
                    break;
                }
                kept.append(token.value());
                width += tokenWidth;
                continue;
            }

            // Text tokens can be trimmed one character at a time.
            boolean stop = false;
            for (int index = 0; index < token.value().length(); index++) {
                int charWidth = metrics.charWidth(token.value().charAt(index));
                if (width + charWidth > budget) {
                    stop = true;
                    break;
                }
                kept.append(token.value().charAt(index));
                width += charWidth;
            }
            if (stop) {
                break;
            }
        }

        draw(graphics, kept.toString().stripTrailing() + ELLIPSIS, x, baseline);
    }

    /**
     * Height of a capital letter above the baseline.
     *
     * <p>Emoji are sized and positioned against this rather than {@link FontMetrics#getAscent()}.
     * Ascent includes headroom for diacritics well above the capitals, so sizing a square glyph to
     * it makes the emoji noticeably larger than the text and pushes it visibly upward.
     */
    private static int capHeight(Graphics2D graphics) {
        Rectangle2D bounds = graphics.getFont()
                .createGlyphVector(graphics.getFontRenderContext(), "H")
                .getVisualBounds();
        int cap = (int) Math.round(-bounds.getY());
        return cap > 0 ? cap : Math.round(graphics.getFontMetrics().getAscent() * 0.7f);
    }

    /** Slightly larger than the capitals, which is how emoji read as matching inline text. */
    private static int emojiSize(Graphics2D graphics) {
        return Math.max(8, Math.round(capHeight(graphics) * 1.18f));
    }

    /**
     * Centres the emoji square on the optical centre of the text — halfway up the capitals —
     * rather than sitting it on the baseline, so it lines up with the words beside it.
     */
    private static int emojiTop(Graphics2D graphics, int baseline, int emojiSize) {
        return Math.round(baseline - capHeight(graphics) / 2f - emojiSize / 2f);
    }

    private static int emojiAdvance(int emojiSize) {
        return emojiSize + Math.max(2, emojiSize / 8);
    }

    private Optional<BufferedImage> image(String sequence) {
        return images.computeIfAbsent(sequence, this::loadImage);
    }

    private Optional<BufferedImage> loadImage(String sequence) {
        String code = toCodePoints(sequence);
        if (code.isEmpty()) {
            return Optional.empty();
        }

        File cached = cacheDirectory == null ? null : new File(cacheDirectory, code + ".png");
        if (cached != null && cached.isFile()) {
            try {
                BufferedImage image = ImageIO.read(cached);
                if (image != null) {
                    return Optional.of(image);
                }
            } catch (IOException ignored) {
                // Corrupt cache entry; fall through and re-fetch.
            }
        }

        try {
            HttpResponse<byte[]> response = http.send(
                    HttpRequest.newBuilder(URI.create(CDN + code + ".png"))
                            .timeout(TIMEOUT)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
            if (image == null) {
                return Optional.empty();
            }
            if (cached != null) {
                Files.write(cached.toPath(), response.body());
            }
            return Optional.of(image);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    /**
     * Twemoji filename for a sequence: hyphen-joined lowercase hex code points, with the
     * presentation selector U+FE0F dropped unless the sequence is a ZWJ sequence.
     */
    static String toCodePoints(String sequence) {
        boolean hasZwj = sequence.codePoints().anyMatch(cp -> cp == ZWJ);
        StringBuilder code = new StringBuilder();
        for (int index = 0; index < sequence.length(); ) {
            int cp = sequence.codePointAt(index);
            index += Character.charCount(cp);
            if (cp == VARIATION_SELECTOR_16 && !hasZwj) {
                continue;
            }
            if (cp == VARIATION_SELECTOR_15) {
                continue;
            }
            if (!code.isEmpty()) {
                code.append('-');
            }
            code.append(Integer.toHexString(cp).toLowerCase(Locale.ROOT));
        }
        return code.toString();
    }

    static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder literal = new StringBuilder();

        int index = 0;
        while (index < text.length()) {
            int cp = text.codePointAt(index);
            int sequenceEnd = emojiSequenceEnd(text, index);

            if (sequenceEnd > index) {
                if (!literal.isEmpty()) {
                    tokens.add(new Token(literal.toString(), false));
                    literal.setLength(0);
                }
                tokens.add(new Token(text.substring(index, sequenceEnd), true));
                index = sequenceEnd;
                continue;
            }

            literal.appendCodePoint(cp);
            index += Character.charCount(cp);
        }

        if (!literal.isEmpty()) {
            tokens.add(new Token(literal.toString(), false));
        }
        return tokens;
    }

    /**
     * End index of the emoji sequence starting at {@code start}, or {@code start} itself when the
     * text there is not emoji. Consumes modifiers, ZWJ joins, keycaps and flag pairs so a
     * multi-code-point emoji stays one unit.
     */
    private static int emojiSequenceEnd(String text, int start) {
        int cp = text.codePointAt(start);

        if (isKeycapBase(cp) && isKeycapSequence(text, start)) {
            int index = start + Character.charCount(cp);
            if (index < text.length() && text.codePointAt(index) == VARIATION_SELECTOR_16) {
                index += Character.charCount(VARIATION_SELECTOR_16);
            }
            return index + Character.charCount(KEYCAP);
        }

        if (!isEmojiBase(cp)) {
            return start;
        }

        int index = start + Character.charCount(cp);
        if (isRegionalIndicator(cp) && index < text.length() && isRegionalIndicator(text.codePointAt(index))) {
            return index + Character.charCount(text.codePointAt(index));
        }

        while (index < text.length()) {
            int next = text.codePointAt(index);
            if (next == VARIATION_SELECTOR_16 || next == VARIATION_SELECTOR_15 || isSkinTone(next)) {
                index += Character.charCount(next);
                continue;
            }
            if (next == ZWJ) {
                int after = index + Character.charCount(next);
                if (after < text.length() && isEmojiBase(text.codePointAt(after))) {
                    index = after + Character.charCount(text.codePointAt(after));
                    continue;
                }
            }
            break;
        }
        return index;
    }

    private static boolean isKeycapBase(int cp) {
        return (cp >= '0' && cp <= '9') || cp == '#' || cp == '*';
    }

    private static boolean isKeycapSequence(String text, int start) {
        int index = start + 1;
        if (index < text.length() && text.codePointAt(index) == VARIATION_SELECTOR_16) {
            index += Character.charCount(VARIATION_SELECTOR_16);
        }
        return index < text.length() && text.codePointAt(index) == KEYCAP;
    }

    private static boolean isSkinTone(int cp) {
        return cp >= 0x1F3FB && cp <= 0x1F3FF;
    }

    private static boolean isRegionalIndicator(int cp) {
        return cp >= 0x1F1E6 && cp <= 0x1F1FF;
    }

    private static boolean isEmojiBase(int cp) {
        return (cp >= 0x1F000 && cp <= 0x1FAFF)
                || (cp >= 0x2600 && cp <= 0x27BF)
                || (cp >= 0x2B00 && cp <= 0x2BFF)
                || cp == 0x21A9
                || cp == 0x21AA
                || cp == 0x00A9
                || cp == 0x00AE
                || cp == 0x2122
                || cp == 0x203C
                || cp == 0x2049
                || cp == 0x3030
                || cp == 0x303D
                || cp == 0x3297
                || cp == 0x3299
                || (cp >= 0x2934 && cp <= 0x2935)
                || (cp >= 0x25AA && cp <= 0x25FE);
    }

    record Token(String value, boolean emoji) {
    }
}
