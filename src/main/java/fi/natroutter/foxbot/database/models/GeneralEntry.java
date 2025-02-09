package fi.natroutter.foxbot.database.models;

import fi.natroutter.foxlib.mongo.MongoData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class GeneralEntry implements MongoData {

    String generalID;
    long lastDailyFox = 0L;
    long lastDailyFoxIndex = 1L;
    int lastPoemIndex = 0;
    long TotalDailyFoxesSend = 1L;

    @Override
    public String id() {
        return generalID;
    }
}
