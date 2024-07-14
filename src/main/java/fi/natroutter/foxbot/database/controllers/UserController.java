package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.MongoCollection;
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

    public UserController() {
        super("users", UserEntry.class);
    }

    public UserEntry findByID(String userID) {
        UserEntry data = findBy("userID", userID);
        if (data == null) {
            MongoCollection<UserEntry> col = getCollection();
            UserEntry newEntry = new UserEntry(userID);
            col.insertOne(newEntry);
            return newEntry;
        } else {
            return data;
        }
    }

    public List<UserEntry> getTopSocial(String userID) {
        MongoCollection<UserEntry> users = getCollection();
        return users.find().sort(Sorts.descending("socialCredits")).limit(10).into(new ArrayList<>());
    }

    public Long getInviteCont(String userID) {
        MongoCollection<UserEntry> users = getCollection();
        return users.countDocuments(Filters.eq("invitedBy", userID));
    }

    @Override
    public void save(UserEntry data) {
        replaceBy("userID", data.getUserID(), data);
    }
}
