package fi.natroutter.foxbot.utilities;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Draws Discord user avatars as circles into a {@link Graphics2D}.
 *
 * <p>Avatar URLs are content-addressed — the hash in the path changes whenever the user changes
 * their picture — so a fetched image is cached in memory and on disk under a digest of its URL and
 * never re-fetched while it is current.
 *
 * <p>A render typically needs a handful of avatars at once, so {@link #prefetch(Collection)} warms
 * them in parallel: a cold cache then costs one round trip in total rather than one per row. When
 * an avatar cannot be fetched at all, {@link #draw} falls back to a coloured initial, which is what
 * Discord itself shows for a user without a picture.
 */
public final class AvatarImageRenderer {

    private static final String CACHE_DIRECTORY = "avatar-cache";
    private static final String CDN_HOST = "cdn.discordapp.com";

    /** Requested from the CDN large enough to downscale cleanly, small enough to stay a few KB. */
    private static final int REQUEST_SIZE = 128;

    /** Discord hands users without a picture one of six default avatars, keyed off their ID. */
    private static final int DEFAULT_AVATAR_COUNT = 6;

    private static final Color[] FALLBACK_COLORS = {
            new Color(0x5865F2), new Color(0x3BA55D), new Color(0xFAA61A),
            new Color(0xED4245), new Color(0x9B59B6), new Color(0x1ABC9C)
    };

    /**
     * Deliberately short. Fetches happen while a command is rendering, so an unreachable CDN must
     * fail fast rather than stall the reply; a failure is cached so it costs one attempt per run.
     */
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /** Upper bound on how long a whole prefetch may hold up a render. */
    private static final Duration PREFETCH_BUDGET = Duration.ofSeconds(6);

    private static final ExecutorService FETCHERS = Executors.newFixedThreadPool(6, runnable -> {
        Thread thread = new Thread(runnable, "avatar-fetcher");
        thread.setDaemon(true);
        return thread;
    });

    /** Absent value means "known to be unavailable", so a miss is not retried on every render. */
    private final ConcurrentMap<String, Optional<BufferedImage>> sources = new ConcurrentHashMap<>();

    /** Circle-cropped and scaled results, keyed by source URL and pixel size. */
    private final ConcurrentMap<String, BufferedImage> circles = new ConcurrentHashMap<>();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final File cacheDirectory;

    public AvatarImageRenderer() {
        File directory = new File(CACHE_DIRECTORY);
        this.cacheDirectory = directory.isDirectory() || directory.mkdirs() ? directory : null;
    }

    /**
     * Loads every given avatar concurrently, so the rows that follow draw from cache. Anything that
     * has not arrived within {@link #PREFETCH_BUDGET} is abandoned and falls back to an initial.
     */
    public void prefetch(Collection<String> urls) {
        Set<String> pending = new LinkedHashSet<>();
        for (String url : urls) {
            if (url != null && !url.isBlank() && !sources.containsKey(url)) {
                pending.add(url);
            }
        }
        if (pending.isEmpty()) {
            return;
        }

        List<Future<?>> tasks = new ArrayList<>(pending.size());
        for (String url : pending) {
            tasks.add(FETCHERS.submit(() -> image(url)));
        }

        long deadline = System.nanoTime() + PREFETCH_BUDGET.toNanos();
        for (Future<?> task : tasks) {
            long remaining = deadline - System.nanoTime();
            try {
                task.get(Math.max(0, remaining), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ignored) {
                // Timed out or failed: draw() falls back to an initial for this one.
            }
        }
    }

    /**
     * Draws the avatar as a circle of {@code size} pixels with its top-left corner at
     * {@code (x, y)}, or a coloured initial taken from {@code name} when no image is available.
     */
    public void draw(Graphics2D graphics, String url, String name, int x, int y, int size) {
        BufferedImage circle = url == null || url.isBlank() ? null : circle(url, size);
        if (circle != null) {
            graphics.drawImage(circle, x, y, null);
            return;
        }
        drawInitial(graphics, name, x, y, size);
    }

    /**
     * Avatar URL for a user with no picture of their own, derived from their ID the same way
     * Discord derives it.
     */
    public static String defaultAvatarUrl(String userID) {
        long index = 0;
        try {
            index = Math.floorMod(Long.parseLong(userID) >> 22, DEFAULT_AVATAR_COUNT);
        } catch (NumberFormatException ignored) {
            // Not a snowflake; the first default avatar is as good as any.
        }
        return "https://" + CDN_HOST + "/embed/avatars/" + index + ".png";
    }

    private void drawInitial(Graphics2D graphics, String name, int x, int y, int size) {
        String initial = initial(name);

        graphics.setColor(FALLBACK_COLORS[Math.floorMod(initial.hashCode(), FALLBACK_COLORS.length)]);
        graphics.fill(new Ellipse2D.Float(x, y, size, size));

        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, Math.round(size * 0.5f))));
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.setColor(Color.WHITE);
        graphics.drawString(initial,
                x + (size - metrics.stringWidth(initial)) / 2,
                y + (size - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    private static String initial(String name) {
        if (name == null) {
            return "?";
        }
        for (int index = 0; index < name.length(); ) {
            int cp = name.codePointAt(index);
            if (Character.isLetterOrDigit(cp)) {
                return new String(Character.toChars(Character.toUpperCase(cp)));
            }
            index += Character.charCount(cp);
        }
        return "?";
    }

    private BufferedImage circle(String url, int size) {
        BufferedImage cached = circles.get(url + "|" + size);
        if (cached != null) {
            return cached;
        }

        BufferedImage source = image(url).orElse(null);
        if (source == null) {
            return null;
        }

        BufferedImage circle = toCircle(source, size);
        circles.put(url + "|" + size, circle);
        return circle;
    }

    /** Masks the avatar into a circle rather than clipping it, so the edge stays antialiased. */
    private static BufferedImage toCircle(BufferedImage source, int size) {
        BufferedImage circle = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = circle.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        graphics.setColor(Color.WHITE);
        graphics.fill(new Ellipse2D.Float(0, 0, size, size));
        graphics.setComposite(AlphaComposite.SrcIn);
        graphics.drawImage(source, 0, 0, size, size, null);
        graphics.dispose();
        return circle;
    }

    private Optional<BufferedImage> image(String url) {
        Optional<BufferedImage> cached = sources.get(url);
        if (cached != null) {
            return cached;
        }
        Optional<BufferedImage> loaded = loadImage(url);
        sources.put(url, loaded);
        return loaded;
    }

    private Optional<BufferedImage> loadImage(String url) {
        File cached = cacheDirectory == null ? null : new File(cacheDirectory, digest(url) + ".img");
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
                    HttpRequest.newBuilder(URI.create(sized(url)))
                            .timeout(TIMEOUT)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (image == null) {
                return Optional.empty();
            }
            if (cached != null) {
                Files.write(cached.toPath(), response.body());
            }
            return Optional.of(image);
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    /** Discord serves avatars at 1024px by default; asking for a small one keeps the fetch cheap. */
    private static String sized(String url) {
        if (url.indexOf('?') >= 0 || !url.contains(CDN_HOST)) {
            return url;
        }
        return url + "?size=" + REQUEST_SIZE;
    }

    private static String digest(String url) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(sha1.digest(url.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(url.hashCode()).toLowerCase(Locale.ROOT);
        }
    }
}
