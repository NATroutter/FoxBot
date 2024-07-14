package fi.natroutter.foxbot.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public abstract class ModelController<T> extends MongoConnector {

    private final String collectionName;
    private final Class<T> clazz;

    public ModelController(String collectionName, Class<T> clazz) {
        this.collectionName = collectionName;
        this.clazz = clazz;

        //Check if collection exists, if not create it!
        MongoDatabase db = getDatabase();
        List<String> databaseNames = getDatabase().listCollectionNames().into(new ArrayList<>());
        if (!databaseNames.contains(collectionName)) {
            db.createCollection(collectionName);
        }

    }

    public MongoCollection<T> getCollection() {
        return getDatabase().getCollection(collectionName, clazz);
    }

    public T findBy(String fieldName, Object fieldValue) {
        MongoCollection<T> entries = getCollection();
        return entries.find(Filters.eq(fieldName, fieldValue)).first();
    }

    public void replaceBy(String fieldName, Object fieldValue, T data) {
        MongoDatabase db = getDatabase();
        MongoCollection<T> collection = db.getCollection(collectionName, clazz);
        collection.findOneAndReplace(Filters.eq(fieldName, fieldValue), data);
    }

    public abstract void save(T data);

}
