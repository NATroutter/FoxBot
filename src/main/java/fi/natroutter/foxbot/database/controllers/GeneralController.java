package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.database.ModelController;
import fi.natroutter.foxbot.database.MongoConnector;
import fi.natroutter.foxbot.database.models.GeneralEntry;
import fi.natroutter.foxbot.database.models.GroupEntry;
import org.bson.conversions.Bson;

import java.util.function.Consumer;

public class GeneralController extends ModelController<GeneralEntry> {

    public GeneralController() {
        super("general", GeneralEntry.class);

        MongoCollection<GeneralEntry> col = getCollection();
        if (!(col.countDocuments() > 0)) {
            col.insertOne(new GeneralEntry());
        }
    }

    public GeneralEntry get() {
        return getCollection().find().first();
    }

    @Override
    public void save(GeneralEntry data) {
        getCollection().findOneAndReplace(Filters.empty(), data);
    }

}
