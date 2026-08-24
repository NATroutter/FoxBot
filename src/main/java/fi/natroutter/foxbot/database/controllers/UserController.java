package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxlib.mongo.ModelController;
import fi.natroutter.foxlib.mongo.MongoConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class UserController extends ModelController<UserEntry> {

    private final AtomicBoolean indexesEnsured = new AtomicBoolean(false);

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

    /**
     * Every balance, most credits first.
     *
     * <p>Deliberately not capped: the leaderboard pages through the whole board, and it drops bots
     * and users the bot can no longer see afterwards, so any cap here would decide how deep the
     * board goes by accident. Sorted on an index so the scan stays cheap as the collection grows.
     */
    public void getTopSocial(Consumer<List<UserEntry>> entry) {
        getCollection(users-> {
            ensureIndexes(users);
            entry.accept(users.find().sort(Sorts.descending("socialCredits")).into(new ArrayList<>()));
        });
    }

    private void ensureIndexes(MongoCollection<UserEntry> users) {
        if (!indexesEnsured.compareAndSet(false, true)) {
            return;
        }
        users.createIndex(Indexes.descending("socialCredits"));
    }

    public void getInviteCont(String userID, Consumer<Long> count) {
        getCollection(users-> {
            count.accept(users.countDocuments(Filters.eq("invitedBy", userID)));
        });
    }
}
