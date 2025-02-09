package fi.natroutter.foxbot.database.models;

import fi.natroutter.foxlib.mongo.MongoData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class GroupEntry implements MongoData {

    String groupID;
    List<String> permissions;

    public GroupEntry(String groupID) {
        this.groupID=groupID;
        this.permissions = new ArrayList<>();
    }

    @Override
    public String id() {
        return groupID;
    }
}
