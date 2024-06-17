package fi.natroutter.foxbot.database.controllers;

import fi.natroutter.foxbot.database.ModelController;
import fi.natroutter.foxbot.database.MongoConnector;
import fi.natroutter.foxbot.database.models.GroupEntry;

import java.util.function.Consumer;

public class GroupController extends ModelController<GroupEntry> {

    public GroupController(MongoConnector connector) {
        super(connector, "groups", GroupEntry.class);
    }

    public void findByID(String groupID, Consumer<GroupEntry> entry) {
        findBy("groupID", groupID, data-> {
            if (data == null) {
                getCollection(col->{
                    GroupEntry newEntry = new GroupEntry(groupID);
                    col.insertOne(newEntry);
                    entry.accept(newEntry);
                });
            } else {
                entry.accept(data);
            }
        });
    }

    @Override
    public void save(Object data) {
        if (data instanceof GroupEntry entry) {
            replaceBy("groupID", entry.getGroupID(), entry);
        }
    }
}
