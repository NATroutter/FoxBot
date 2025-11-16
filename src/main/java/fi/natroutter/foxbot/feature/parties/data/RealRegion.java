package fi.natroutter.foxbot.feature.parties.data;

import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.internal.utils.EntityString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RealRegion {

    AUTOMATIC("automatic", "Automatic", null),
    BRAZIL("brazil", "Brazil", "\ud83c\udde7\ud83c\uddf7"),
    HONG_KONG("hongkong", "Hong Kong", "\ud83c\udded\ud83c\uddf0"),
    INDIA("india", "India", "\ud83c\uddee\ud83c\uddf3"),
    JAPAN("japan", "Japan", "\ud83c\uddef\ud83c\uddf5"),
    ROTTERDAM("rotterdam", "Rotterdam", "\ud83c\uddf3\ud83c\uddf1"),
    SINGAPORE("singapore", "Singapore", "\ud83c\uddf8\ud83c\uddec"),
    SOUTH_AFRICA("southafrica", "South Africa", "\ud83c\uddff\ud83c\udde6"),
    SYDNEY("sydney", "Sydney", "\ud83c\udde6\ud83c\uddfa"),
    US_CENTRAL("us-central", "US Central", "\ud83c\uddfa\ud83c\uddf8"),
    US_EAST("us-east", "US East", "\ud83c\uddfa\ud83c\uddf8"),
    US_SOUTH("us-south", "US South", "\ud83c\uddfa\ud83c\uddf8"),
    US_WEST("us-west", "US West", "\ud83c\uddfa\ud83c\uddf8"),
    UNKNOWN("", "Unknown Region", null),

    ;

    private final String key;
    private final String name;
    private final String emoji;

    private RealRegion(String key, String name, String emoji) {
        this.key = key;
        this.name = name;
        this.emoji = emoji;
    }

    @Nonnull
    public String getName() {
        return this.name;
    }

    @Nonnull
    public String getKey() {
        return this.key;
    }

    @Nullable
    public String getEmoji() {
        return this.emoji;
    }

    public Region toDiscord() {
        if (this == UNKNOWN) return Region.AUTOMATIC;

        for(Region region : Region.values()) {
            if (region.getKey().equals(this.getKey())) {
                return region;
            }
        }
        return Region.AUTOMATIC;
    }

    public static String regionList() {
        return Arrays.stream(values()).map(RealRegion::getKey).collect(Collectors.joining(", "));
    }

    public static List<Command.Choice> choices() {
        return Arrays.stream(values()).map(v-> new Command.Choice(v.name, v.key)).toList();
    }

    public static RealRegion fromDiscord(Region region) {
        for(RealRegion rr : values()) {
            if (rr.getKey().equals(region.getKey())) {
                return rr;
            }
        }
        return AUTOMATIC;
    }

    @Nonnull
    public static RealRegion fromKey(@Nullable String key) {
        for(RealRegion region : values()) {
            if (region.getKey().equals(key)) {
                return region;
            }
        }
        return AUTOMATIC;
    }

    @Nullable
    public static RealRegion fromKeyNullable(@Nullable String key) {
        for(RealRegion region : values()) {
            if (region.getKey().equals(key)) {
                return region;
            }
        }
        return null;
    }

    public String toString() {
        return (new EntityString(this)).setType(this).toString();
    }
}
