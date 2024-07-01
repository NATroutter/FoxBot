package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import fi.natroutter.foxbot.database.ModelController;
import fi.natroutter.foxbot.database.MongoConnector;
import fi.natroutter.foxbot.database.models.UserEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class UserController extends ModelController<UserEntry> {

    public UserController(MongoConnector connector) {
        super(connector, "users", UserEntry.class);
    }

    public void findByID(String userID, Consumer<UserEntry> entry) {
        findBy("userID", userID, data-> {
            if (data == null) {
                getCollection(col->{
                    UserEntry newEntry = new UserEntry(userID);
                    col.insertOne(newEntry);
                    entry.accept(newEntry);
                });
            } else {
                entry.accept(data);
            }
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

    @Override
    public void save(Object data) {
        if (data instanceof UserEntry entry) {
            replaceBy("userID", entry.getUserID(), entry);
        }
    }
}
