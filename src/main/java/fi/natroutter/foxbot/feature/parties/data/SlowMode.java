package fi.natroutter.foxbot.feature.parties.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SlowMode {

    DISABLED("off", 0),
    SECONDS_5("5s", 5),
    SECONDS_10("10s", 10),
    SECONDS_15("15s", 15),
    SECONDS_30("30s", 30),

    MINUTES_1("1m", 60),
    MINUTES_2("2m", 60*2),
    MINUTES_5("5m", 60*5),
    MINUTES_10("10m", 60*10),
    MINUTES_15("15m", 60*15),
    MINUTES_30("30m", 60*30),

    HOURS_1("1h", 60*60),
    HOURS_2("2h", 60*60*2),
    HOURS_6("6h", 60*60*6),

    ;

    private String arg;
    private int value;

    public static SlowMode fromArg(String arg) {
        return Arrays.stream(SlowMode.values())
                .filter(value -> value.getArg().equalsIgnoreCase(arg))
                .findFirst()
                .orElse(null);
    }
    public static SlowMode fromValue(int value) {
        return Arrays.stream(SlowMode.values())
                .filter(mode -> mode.getValue() == value)
                .findFirst()
                .orElse(null);
    }

}
