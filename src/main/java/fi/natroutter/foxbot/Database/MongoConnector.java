package fi.natroutter.foxbot.Database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.utilities.NATLogger;
import fi.natroutter.foxbot.utilities.Utils;
import lombok.Getter;
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
    private NATLogger logger = FoxBot.getLogger();

    @Getter
    private boolean validConfig = false;

    private List<String> collections = new ArrayList<>();

    private String database;
    private String username;
    private String password;
    private String host;
    private int port;

    public MongoConnector() {
        validConfig = validateConfig();
    }

    public void registerCollection(String name) {
        collections.add(name);
    }

    protected void getDatabase(Consumer<MongoDatabase> action) {
        if (!validateConfig()) {return;}

        String uri = "mongodb://"+username+":"+password+"@"+host+":"+port+"/";

        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            if (!mongoClient.listDatabaseNames().into(new ArrayList<>()).contains(database)) {
                logger.error("MongoDB Error : database " + database + " doesn't exists!");
                return;
            }
            MongoDatabase mdb = mongoClient.getDatabase(database).withCodecRegistry(pojoCodecRegistry);

            List<String> databaseNames = mdb.listCollectionNames().into(new ArrayList<>());
            for (String col : collections) {
                if (!databaseNames.contains(col)) {
                    mdb.createCollection(col);
                }
            }

            action.accept(mdb);
        } catch (Exception e) {
            logger.error("MongoDB Error : " + e.getMessage());
        }
    }

    private boolean validateConfig() {
        database = config.get().getMongoDB().getDatabase();
        if (!Utils.validateConf(database)) {
            logger.error("Invalid MongoDB configuration : missing/invalid database!");
            return false;
        }
        username = config.get().getMongoDB().getUsername();
        if (!Utils.validateConf(username)) {
            logger.error("Invalid MongoDB configuration : missing/invalid username!");
            return false;
        }
        password = config.get().getMongoDB().getPassword();
        if (!Utils.validateConf(password)) {
            logger.error("Invalid MongoDB configuration : missing/invalid password!");
            return false;
        }
        host = config.get().getMongoDB().getHost();
        if (!Utils.validateConf(host)) {
            logger.error("Invalid MongoDB configuration : missing/invalid host!");
            return false;
        }
        port = config.get().getMongoDB().getPort();
        if (!Utils.validateConf(port)) {
            logger.error("Invalid MongoDB configuration : missing/invalid port!");
            return false;
        }
        return true;
    }

}
