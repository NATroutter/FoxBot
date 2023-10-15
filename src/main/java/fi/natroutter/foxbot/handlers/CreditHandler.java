package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.UserEntry;
import net.dv8tion.jda.api.entities.User;

import java.util.List;
import java.util.function.Consumer;

public class CreditHandler {

    private MongoHandler mongo = FoxBot.getMongo();

    public void set(User user, int amount) {
        mongo.getUserByID(user.getId(), entry -> {
            entry.setSocialCredits(amount);
            mongo.save(entry);
        });
    }

    public void add(User user, int amount) {
        mongo.getUserByID(user.getId(), entry -> {
            entry.setSocialCredits(entry.getSocialCredits() + amount);
            mongo.save(entry);
        });
    }

    public void take(User user, int amount) {
        mongo.getUserByID(user.getId(), entry -> {
            entry.setSocialCredits(entry.getSocialCredits() - amount);
            mongo.save(entry);
        });
    }

    public void get(User user, Consumer<Long> action) {
        mongo.getUserByID(user.getId(), data -> {
            action.accept(data.getSocialCredits());
        });
    }

    public void top10(User user, Consumer<List<UserEntry>> action) {
        mongo.getTopSocial(user.getId(), action);
    }

}
