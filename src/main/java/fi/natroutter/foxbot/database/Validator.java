package fi.natroutter.foxbot.database;

import com.mongodb.client.MongoCollection;

public class Validator {

    public GroupEntry group(MongoCollection<GroupEntry> groups, String groupID, GroupEntry entry) {
        if (entry == null) {
            entry = new GroupEntry(groupID);
            groups.insertOne(entry);
        }
        return entry;
    }

    public UserEntry user(MongoCollection<UserEntry> users, String userID, UserEntry entry) {
        if (entry == null) {
            entry = new UserEntry(userID);
            users.insertOne(entry);
        }
        return entry;
    }

}
