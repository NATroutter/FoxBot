package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.UserEntry;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.function.Consumer;

public class CreditHandler {

    private MongoHandler mongo = FoxBot.getMongo();

    public void set(User user, int amount) {
        UserEntry entry = mongo.getUsers().findByID(user.getId());
        entry.setSocialCredits(amount);
        mongo.save(entry);
    }

    public void add(User user, int amount) {
        UserEntry entry = mongo.getUsers().findByID(user.getId());
        entry.setSocialCredits(entry.getSocialCredits() + amount);
        mongo.save(entry);
    }

    public void take(User user, int amount) {
        UserEntry entry = mongo.getUsers().findByID(user.getId());
        entry.setSocialCredits(entry.getSocialCredits() - amount);
        mongo.save(entry);
    }

    public long get(User user) {
        UserEntry entry = mongo.getUsers().findByID(user.getId());
        return entry.getSocialCredits();
    }

    public List<UserEntry> top10(User user) {
        return mongo.getUsers().getTopSocial(user.getId());
    }

}
