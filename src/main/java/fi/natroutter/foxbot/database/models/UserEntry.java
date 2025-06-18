package fi.natroutter.foxbot.database.models;

import fi.natroutter.foxlib.mongo.MongoData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class UserEntry implements MongoData {

    String userID;
    long socialCredits;
    String invitedBy;

    public UserEntry(String userID) {
        this.userID = userID;
        this.socialCredits = 0;
        this.invitedBy = "0";
    }

    @Override
    public String id() {
        return userID;
    }
}
