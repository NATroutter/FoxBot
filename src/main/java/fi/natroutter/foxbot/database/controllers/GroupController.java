package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.MongoCollection;
import fi.natroutter.foxbot.database.ModelController;
import fi.natroutter.foxbot.database.MongoConnector;
import fi.natroutter.foxbot.database.models.GroupEntry;

import java.util.function.Consumer;

public class GroupController extends ModelController<GroupEntry> {

    public GroupController() {
        super("groups", GroupEntry.class);
    }

    public GroupEntry findByID(String groupID) {
        GroupEntry data = findBy("groupID", groupID);
        if (data == null) {
            MongoCollection<GroupEntry> col = getCollection();
            GroupEntry newEntry = new GroupEntry(groupID);
            col.insertOne(newEntry);
            return newEntry;
        } else {
            return data;
        }
    }

    @Override
    public void save(GroupEntry data) {
        replaceBy("groupID", data.getGroupID(), data);
    }
}
