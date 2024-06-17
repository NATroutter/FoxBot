package fi.natroutter.foxbot.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.Config;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoConnector {

    private ConfigProvider config = FoxBot.getConfig();
    private FoxLogger logger = FoxBot.getLogger();

    public void getDatabase(Consumer<MongoDatabase> action) {
        if (!config.isInitialized()) {
            logger.error("MongoDB Error : Configuration is not initialized!");
            return;
        }
        Config.MongoDB cfg = config.get().getMongoDB();

        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));

        try (MongoClient mongoClient = MongoClients.create(cfg.getUri())) {
            if (!mongoClient.listDatabaseNames().into(new ArrayList<>()).contains(cfg.getDatabase())) {
                logger.error("MongoDB Error : database " + cfg.getDatabase() + " doesn't exists!");
                return;
            }
            MongoDatabase mdb = mongoClient.getDatabase(cfg.getDatabase()).withCodecRegistry(pojoCodecRegistry);

            action.accept(mdb);
        } catch (Exception e) {
            logger.error("MongoDB Error : " + e.getMessage());
        }
    }

}
