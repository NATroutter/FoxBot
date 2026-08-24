package fi.natroutter.foxbot.feature.stickers.data;

import java.io.File;

public record StickerDescriptor(String id, String name, File file) {

    public long cacheVersion() {
        if (file == null) {
            return -1;
        }
        return file.lastModified() ^ file.length();
    }
}
