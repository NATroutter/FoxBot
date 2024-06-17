package fi.natroutter.foxbot.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public abstract class ModelController<T> {

    private final MongoConnector connector;
    private final String collectionName;
    private final Class<T> clazz;

    public ModelController(MongoConnector connector, String collectionName, Class<T> clazz) {
        this.connector = connector;
        this.collectionName = collectionName;
        this.clazz = clazz;

        //Check if collection exists, if not create it!
        connector.getDatabase(db ->{
            List<String> databaseNames = db.listCollectionNames().into(new ArrayList<>());
            if (!databaseNames.contains(collectionName)) {
                db.createCollection(collectionName);
            }
        });
    }

    public void getCollection(Consumer<MongoCollection<T>> data) {
        getConnector().getDatabase(db-> {
            data.accept(db.getCollection(collectionName, clazz));
        });
    }

    public void findBy(String field, Object value, Consumer<T> data) {
        getCollection(entries->{
            T entry = entries.find(Filters.eq(field, value)).first();
            data.accept(entry);
        });
    }

    public void replaceBy(String field, Object value, T data) {
        getConnector().getDatabase(db->{
            MongoCollection<T> collection = db.getCollection(collectionName, clazz);
            collection.findOneAndReplace(Filters.eq(field, value), data);
        });
    }

    public abstract void save(Object data);

}
