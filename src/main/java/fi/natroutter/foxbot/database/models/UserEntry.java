package fi.natroutter.foxbot.database.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class UserEntry {

    String userID;
    long socialCredits;
    String invitedBy;

    public UserEntry(String userID) {
        this.userID = userID;
        this.socialCredits = 0;
        this.invitedBy = "0";
    }

}
