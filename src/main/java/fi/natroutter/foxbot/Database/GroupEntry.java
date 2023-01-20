package fi.natroutter.foxbot.Database;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class GroupEntry {

    String groupID;
    List<String> permissions;

    public GroupEntry() {}

    public GroupEntry(String groupID) {
        this.groupID=groupID;
        this.permissions = new ArrayList<>();
    }

}
