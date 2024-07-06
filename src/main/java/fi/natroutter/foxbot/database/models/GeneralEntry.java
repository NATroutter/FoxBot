package fi.natroutter.foxbot.database.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class GeneralEntry {

    long lastDailyFox = 0L;
    long lastDailyFoxIndex = 1L;
    int lastPoemIndex = 0;
    long TotalDailyFoxesSend = 0L;

}
