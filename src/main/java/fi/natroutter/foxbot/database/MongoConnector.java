package fi.natroutter.foxbot.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoConnector implements AutoCloseable {

    private ConfigProvider config = FoxBot.getConfig();
    private FoxLogger logger = FoxBot.getLogger();

    private Config.MongoDB cfg;

    private MongoClient client;

    public MongoConnector() {
        initConnection();
    }

    public void initConnection() {
        if (!config.isInitialized()) {
            logger.error("MongoDB Error : Configuration is not initialized!");
            return;
        }
        cfg = config.get().getMongoDB();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(cfg.getUri()))
                .applyToConnectionPoolSettings(b ->
                        b.maxConnecting(50).maxSize(1000)
                )
                .build();

        try {
            client = MongoClients.create(settings);
            if (!client.listDatabaseNames().into(new ArrayList<>()).contains(cfg.getDatabase())) {
                logger.error("MongoDB Error : database " + cfg.getDatabase() + " doesn't exists!");
            }
        } catch (MongoSecurityException e) {
            logger.error("MongoDB Error : Failed to authenticate, check your config!");

        } catch (Exception e) {
            logger.error("MongoDB Error : " + e.getMessage());
        }
    }

    private CodecRegistry pojoCodec() {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        return fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
    }

    public MongoDatabase getDatabase() {
        if (client == null) {
            initConnection();
        }
        return client.getDatabase(cfg.getDatabase()).withCodecRegistry(pojoCodec());
    }

    @Override
    public void close() throws Exception {
        client.close();
        client = null;
    }







//    public MongoDatabase getDatabase() {
//        if (!config.isInitialized()) {
//            logger.error("MongoDB Error : Configuration is not initialized!");
//            return null;
//        }
//        Config.MongoDB cfg = config.get().getMongoDB();
//
//        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
//        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
//
//        try (MongoClient mongoClient = MongoClients.create(cfg.getUri())) {
//            if (!mongoClient.listDatabaseNames().into(new ArrayList<>()).contains(cfg.getDatabase())) {
//                logger.error("MongoDB Error : database " + cfg.getDatabase() + " doesn't exists!");
//                return null;
//            }
//            MongoDatabase db = mongoClient.getDatabase(cfg.getDatabase()).withCodecRegistry(pojoCodecRegistry);
//            return db;
//        } catch (MongoSecurityException e) {
//            logger.error("MongoDB Error : Failed to authenticate, check your config!");
//
//        } catch (Exception e) {
//            logger.error("MongoDB Error : " + e.getMessage());
//        }
//        return null;
//    }

}
