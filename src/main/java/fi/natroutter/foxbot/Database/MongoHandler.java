package fi.natroutter.foxbot.Database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import java.util.function.Consumer;

public class MongoHandler extends MongoConnector {

    public MongoHandler() {
        super();
        registerCollection("groups");
    }

    public GroupEntry validateGroup(MongoCollection<GroupEntry> groups, String groupID, GroupEntry group) {
        if (group == null) {
            group = new GroupEntry(groupID);
            groups.insertOne(group);
        }
        return group;
    }

    public void getGroupByID(String groupID, Consumer<GroupEntry> entry) {
        getGroups(groups-> {
            GroupEntry group = validateGroup(groups, groupID, groups.find(Filters.eq("groupID", groupID)).first());
            entry.accept(group);
        });
    }

    public void getGroups(Consumer<MongoCollection<GroupEntry>> action) {
        getDatabase(db->{
            action.accept(db.getCollection("groups", GroupEntry.class));
        });
    }

    public void saveGroup(GroupEntry entry) {
        getDatabase(db->{
            MongoCollection<GroupEntry> users = (db.getCollection("groups", GroupEntry.class));
            users.findOneAndReplace(Filters.eq("groupID", entry.groupID), entry);
        });
    }

}
