package fi.natroutter.foxbot.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MongoHandler extends MongoConnector {

    private Validator validator = new Validator();

    public MongoHandler() {
        super();
        registerCollection("groups");
        registerCollection("users");
    }

    //---------- [ Handle groups database] ----------
    public void getGroups(Consumer<MongoCollection<GroupEntry>> action) {
        getDatabase(db->{
            action.accept(db.getCollection("groups", GroupEntry.class));
        });
    }
    public void getGroupByID(String groupID, Consumer<GroupEntry> entry) {
        getGroups(groups-> {
            GroupEntry group = validator.group(groups, groupID, groups.find(Filters.eq("groupID", groupID)).first());
            entry.accept(group);
        });
    }



    //---------- [ Handle users database] ----------
    public void getUsers(Consumer<MongoCollection<UserEntry>> action) {
        getDatabase(db->{
            action.accept(db.getCollection("users", UserEntry.class));
        });
    }
    public void getUserByID(String userID, Consumer<UserEntry> entry) {
        getUsers(users-> {
            UserEntry user = validator.user(users, userID, users.find(Filters.eq("userID", userID)).first());
            entry.accept(user);
        });
    }
    public void getTopSocial(String userID, Consumer<List<UserEntry>> entry) {
        getUsers(users-> {
            entry.accept(users.find().sort(Sorts.descending("socialCredits")).limit(10).into(new ArrayList<>()));
        });
    }

    public void getInviteCont(String userID, Consumer<Long> count) {
        getUsers(users-> {
            count.accept(users.countDocuments(Filters.eq("invitedBy", userID)));
        });
    }



    //---------- [ General Handlers] ----------
    public void save(Object obj) {
        if (obj instanceof GroupEntry entry) {
            getDatabase(db->{
                MongoCollection<GroupEntry> users = (db.getCollection("groups", GroupEntry.class));
                users.findOneAndReplace(Filters.eq("groupID", entry.groupID), entry);
            });
        } else if (obj instanceof UserEntry entry) {
            getDatabase(db->{
                MongoCollection<UserEntry> users = (db.getCollection("users", UserEntry.class));
                users.findOneAndReplace(Filters.eq("userID", entry.userID), entry);
            });
        }
    }

}
