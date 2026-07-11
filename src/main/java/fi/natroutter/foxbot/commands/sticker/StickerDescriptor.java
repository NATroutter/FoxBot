package fi.natroutter.foxbot.commands.sticker;

import java.io.File;

record StickerDescriptor(String id, String name, File file) {

    long cacheVersion() {
        if (file == null) {
            return -1;
        }
        return file.lastModified() ^ file.length();
    }
}
