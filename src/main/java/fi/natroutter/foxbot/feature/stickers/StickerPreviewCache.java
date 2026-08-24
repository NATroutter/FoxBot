package fi.natroutter.foxbot.feature.stickers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.feature.stickers.data.StickerDescriptor;
import fi.natroutter.foxbot.feature.stickers.listeners.StickerPickerListener;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.http.AssetRegistry;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class StickerPreviewCache {

    private static final String ASSET_PREFIX = "stickers/preview";
    private static final String CONTENT_TYPE = "image/png";

    private final ConcurrentMap<String, byte[]> pages = new ConcurrentHashMap<>();
    private final StickerPreviewRenderer renderer = new StickerPreviewRenderer();
    private volatile String version = UUID.randomUUID().toString().replace("-", "");
    private volatile int generatedPages = 0;

    private byte[] getOrRender(List<StickerDescriptor> stickers) throws IOException {
        String key = cacheKey(stickers);
        byte[] cached = pages.get(key);
        if (cached != null) {
            return cached;
        }

        byte[] rendered = renderer.renderPage(stickers);
        byte[] existing = pages.putIfAbsent(key, rendered);
        return existing != null ? existing : rendered;
    }

    public synchronized void rebuildAssets(List<StickerDescriptor> stickers) throws IOException {
        pages.clear();
        AssetRegistry.clearPrefix(ASSET_PREFIX);
        version = UUID.randomUUID().toString().replace("-", "");

        int totalPages = (stickers.size() + StickerPickerListener.PAGE_SIZE - 1) / StickerPickerListener.PAGE_SIZE;
        generatedPages = totalPages;
        for (int page = 0; page < totalPages; page++) {
            int start = page * StickerPickerListener.PAGE_SIZE;
            int end = Math.min(start + StickerPickerListener.PAGE_SIZE, stickers.size());
            List<StickerDescriptor> pageStickers = new ArrayList<>(stickers.subList(start, end));
            byte[] rendered = getOrRender(pageStickers);
            AssetRegistry.put(assetPath(page), rendered, CONTENT_TYPE);
        }
    }

    public String urlForPage(int page) throws IOException {
        Config.HttpServer httpServer = FoxBot.getConfigProvider().get().getHttpServer();
        if (httpServer == null || !httpServer.isEnabled() || httpServer.getPublicAddress() == null || httpServer.getPublicAddress().isBlank()) {
            throw new IOException("HTTP server publicAddress is not configured.");
        }
        if (page < 0 || page >= generatedPages) {
            throw new IOException("Sticker preview page has not been generated: " + (page + 1));
        }
        return joinUrl(publicBaseUrl(httpServer), "assets/" + assetPath(page)) + "?v=" + version;
    }

    private String assetPath(int page) {
        return ASSET_PREFIX + "/" + (page + 1) + ".png";
    }

    private String cacheKey(List<StickerDescriptor> stickers) {
        StringBuilder key = new StringBuilder();
        for (StickerDescriptor sticker : stickers) {
            key.append(sticker.id())
                    .append(':')
                    .append(sticker.cacheVersion())
                    .append('|');
        }
        return key.toString();
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return base + "/" + normalizedPath;
    }

    private String publicBaseUrl(Config.HttpServer httpServer) {
        String configured = httpServer.getPublicAddress().trim();
        try {
            URI uri = new URI(configured);
            String host = uri.getHost();
            if (uri.getPort() == -1 && isLocalHost(host) && !isDefaultPort(uri.getScheme(), httpServer.getPort())) {
                return new URI(
                        uri.getScheme(),
                        uri.getUserInfo(),
                        host,
                        httpServer.getPort(),
                        trimTrailingSlash(uri.getPath()),
                        uri.getQuery(),
                        uri.getFragment()
                ).toString();
            }
        } catch (URISyntaxException ignored) {
        }
        return configured;
    }

    private boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host);
    }

    private boolean isDefaultPort(String scheme, int port) {
        return ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
    }

    private String trimTrailingSlash(String path) {
        if (path == null || path.equals("/")) {
            return "";
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
