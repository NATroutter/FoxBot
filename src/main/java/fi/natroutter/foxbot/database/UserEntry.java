package fi.natroutter.foxbot.database;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter @Setter
public class UserEntry {

    String userID;
    long socialCredits;
    String invitedBy;

    public UserEntry() {}

    public UserEntry(String userID) {
        this.userID = userID;
        this.socialCredits = 0;
        this.invitedBy = "0";
    }

}
