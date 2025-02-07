package fi.natroutter.foxbot.database;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoSecurityException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MongoConnector {

    private FoxLogger logger = FoxBot.getLogger();

    private final Config.MongoDB cfg;

    public MongoConnector(Config.MongoDB cfg) {
        this.cfg = cfg;
    }

    public void getDatabase(Consumer<MongoDatabase> action) {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(pojoCodecProvider));

        try (MongoClient mongoClient = MongoClients.create(cfg.getUri())) {
            if (!mongoClient.listDatabaseNames().into(new ArrayList<>()).contains(cfg.getDatabase())) {
                logger.error("MongoDB Error : database " + cfg.getDatabase() + " doesn't exists!");
                return;
            }
            MongoDatabase mdb = mongoClient.getDatabase(cfg.getDatabase()).withCodecRegistry(pojoCodecRegistry);

            action.accept(mdb);
        } catch (MongoSecurityException e) {
            logger.error("MongoDB Error : Failed to authenticate, check your config!");

        } catch (Exception e) {
            logger.error("MongoDB Error : " + e.getMessage());
            e.printStackTrace();
        }
    }

}
