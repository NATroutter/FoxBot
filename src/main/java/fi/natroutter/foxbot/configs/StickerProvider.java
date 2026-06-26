package fi.natroutter.foxbot.configs;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.DirectoryManager;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class StickerProvider {

    private static final FoxLogger logger = FoxBot.getLogger();

    private static final String directory = "stickers";

    private File stickerDirectory;

    @Getter
    private final ConcurrentHashMap<String, File> stickers = new ConcurrentHashMap<>();

    @Getter
    private boolean initialized = false;

    public StickerProvider() {
        new DirectoryManager.Builder()
                .setSubDirectory(directory)
                .onInfoLog(logger::info)
                .onErrorLog(logger::error)
                .onInitialized(file -> {
                    stickerDirectory = file;
                    initialized = true;
                })
                .build();

        reload();
    }

    public void reload() {
        stickers.clear();

        if (stickerDirectory == null) {
            return;
        }

        File[] files = stickerDirectory.listFiles(File::isFile);

        if (files == null) {
            return;
        }

        for (File file : files) {
            stickers.put(FoxLib.getBasename(file), file);
        }
    }

    public ConcurrentHashMap<String, File> get() {
        return stickers;
    }

    public File get(String name) {
        return stickers.getOrDefault(name, null);
    }
}
