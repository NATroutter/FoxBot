package fi.natroutter.foxbot.http;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AssetRegistry {

    private static final ConcurrentMap<String, Asset> ASSETS = new ConcurrentHashMap<>();

    private AssetRegistry() {
    }

    public static void put(String path, byte[] data, String contentType) {
        ASSETS.put(normalize(path), new Asset(Arrays.copyOf(data, data.length), contentType));
    }

    public static Optional<Asset> find(String path) {
        return Optional.ofNullable(ASSETS.get(normalize(path)));
    }

    public static void clearPrefix(String prefix) {
        String normalizedPrefix = normalize(prefix);
        ASSETS.keySet().removeIf(path -> path.equals(normalizedPrefix) || path.startsWith(normalizedPrefix + "/"));
    }

    private static String normalize(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized;
    }

    public record Asset(byte[] data, String contentType) {
        public Asset {
            data = Arrays.copyOf(data, data.length);
        }
    }
}
