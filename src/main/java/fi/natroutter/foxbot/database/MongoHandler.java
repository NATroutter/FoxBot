package fi.natroutter.foxbot.database;

import fi.natroutter.foxbot.database.controllers.GeneralController;
import fi.natroutter.foxbot.database.controllers.GroupController;
import fi.natroutter.foxbot.database.controllers.UserController;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.UserEntry;
import lombok.Getter;

@Getter
public class MongoHandler {

    private GeneralController general;
    private GroupController groups;
    private UserController users;

    public MongoHandler() {
        MongoConnector connector = new MongoConnector();

        general = new GeneralController(connector);
        groups = new GroupController(connector);
        users = new UserController(connector);
    }

    public void save(Object obj) {
        if (obj instanceof GroupEntry entry) {
            groups.save(entry);
        } else if (obj instanceof UserEntry entry) {
            users.save(entry);
        }
    }

}
