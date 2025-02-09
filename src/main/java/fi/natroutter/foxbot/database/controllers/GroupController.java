package fi.natroutter.foxbot.database.controllers;

import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxlib.mongo.ModelController;
import fi.natroutter.foxlib.mongo.MongoConnector;

import java.util.function.Consumer;

public class GroupController extends ModelController<GroupEntry> {

    public GroupController(MongoConnector connector) {
        super(connector, "groups", "groupID", GroupEntry.class);
    }

    @Override
    public void findByID(String id, Consumer<GroupEntry> entry) {
        super.findByID(id, data-> {
            if (data == null) {
                data = new GroupEntry(id);
                save(data);
            }
            entry.accept(data);
        });
    }
}
