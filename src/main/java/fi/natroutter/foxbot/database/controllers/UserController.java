package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxlib.mongo.ModelController;
import fi.natroutter.foxlib.mongo.MongoConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UserController extends ModelController<UserEntry> {

    public UserController(MongoConnector connector) {
        super(connector, "users", "userID",UserEntry.class);
    }

    @Override
    public void findByID(String id, Consumer<UserEntry> entry) {
        super.findByID(id, data-> {
            if (data == null) {
                data = new UserEntry(id);
                save(data);
            }
            entry.accept(data);
        });
    }

    public void getTopSocial(String userID, Consumer<List<UserEntry>> entry) {
        getCollection(users-> {
            entry.accept(users.find().sort(Sorts.descending("socialCredits")).limit(10).into(new ArrayList<>()));
        });
    }

    public void getInviteCont(String userID, Consumer<Long> count) {
        getCollection(users-> {
            count.accept(users.countDocuments(Filters.eq("invitedBy", userID)));
        });
    }
}
