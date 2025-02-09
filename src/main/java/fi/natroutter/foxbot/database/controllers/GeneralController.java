package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.database.models.GeneralEntry;
import fi.natroutter.foxlib.mongo.ModelController;
import fi.natroutter.foxlib.mongo.MongoConnector;

import java.util.function.Consumer;

public class GeneralController extends ModelController<GeneralEntry> {

    public GeneralController(MongoConnector connector) {
        super(connector, "general", "generalID",GeneralEntry.class);
        getCollection(col -> {
            if (!(col.countDocuments() > 0)) {
                col.insertOne(new GeneralEntry());
            }
        });
    }

    public void get(Consumer<GeneralEntry> entry) {
        getFirst(entry);
    }
}
