package fi.natroutter.foxbot.commands.sticker;

import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.List;

enum StickerSize {
    SMALL("small", 80),
    NORMAL("normal", 160),
    BIG("big", 512);

    private final String key;
    private final int pixels;

    StickerSize(String key, int pixels) {
        this.key = key;
        this.pixels = pixels;
    }

    public String key() {
        return key;
    }

    public int pixels() {
        return pixels;
    }

    public static List<Command.Choice> choices() {
        return List.of(
                new Command.Choice("small", SMALL.key),
                new Command.Choice("normal", NORMAL.key),
                new Command.Choice("big", BIG.key)
        );
    }

    public static StickerSize fromKey(String key) {
        for (StickerSize size : values()) {
            if (size.key.equalsIgnoreCase(key)) {
                return size;
            }
        }
        return NORMAL;
    }
}
