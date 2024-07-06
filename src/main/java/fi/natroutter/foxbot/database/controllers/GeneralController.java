package fi.natroutter.foxbot.database.controllers;

import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.database.ModelController;
import fi.natroutter.foxbot.database.MongoConnector;
import fi.natroutter.foxbot.database.models.GeneralEntry;
import fi.natroutter.foxbot.database.models.GroupEntry;
import org.bson.conversions.Bson;

import java.util.function.Consumer;

public class GeneralController extends ModelController<GeneralEntry> {

    public GeneralController(MongoConnector connector) {
        super(connector, "general", GeneralEntry.class);
        getCollection(col -> {
            if (!(col.countDocuments() > 0)) {
                col.insertOne(new GeneralEntry());
            }
        });
    }

    public void get(Consumer<GeneralEntry> entry) {
        getCollection(col->{
            entry.accept(col.find().first());
        });
    }

    @Override
    public void save(Object data) {
        if (data instanceof GeneralEntry entry) {
            getCollection(col-> {
                col.findOneAndReplace(Filters.empty(), entry);
            });
        }
    }
}
