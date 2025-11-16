package fi.natroutter.foxbot.database;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.database.controllers.GeneralController;
import fi.natroutter.foxbot.database.controllers.GroupController;
import fi.natroutter.foxbot.database.controllers.PartyController;
import fi.natroutter.foxbot.database.controllers.UserController;
import fi.natroutter.foxbot.database.models.GeneralEntry;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.PartyEntry;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.mongo.MongoConfig;
import fi.natroutter.foxlib.mongo.MongoConnector;
import fi.natroutter.foxlib.mongo.MongoData;
import lombok.Getter;

@Getter
public class MongoHandler {

    private GeneralController general;
    private GroupController groups;
    private UserController users;
    private PartyController parties;

    private FoxLogger logger = FoxBot.getLogger();
    private ConfigProvider config = FoxBot.getConfigProvider();

    private boolean initialized = false;

    public MongoHandler() {
        if (!config.isInitialized()) {
            logger.error("MongoDB Error : Configuration is not initialized!");
            return;
        }

        MongoConfig cfg = config.get().getMongoDB();

        if (cfg.getUri().equalsIgnoreCase("URI_HERE") || FoxLib.isBlank(cfg.getUri())) {
            logger.error("MongoDB Error : Database credentials are not configured!");
            return;
        }
        logger.info("Connecting to mongoDB...");

        MongoConnector connector = new MongoConnector(cfg);

        general = new GeneralController(connector);
        groups = new GroupController(connector);
        users = new UserController(connector);
        parties = new PartyController(connector);

        initialized = true;
        logger.info("MongoDB connection established!");
    }

    public <T extends MongoData> void save(T data) {
        if (data instanceof GeneralEntry e) {
            general.save(e);
        } else if (data instanceof UserEntry e) {
            users.save(e);
        } else if (data instanceof GroupEntry e) {
            groups.save(e);
        } else if (data instanceof PartyEntry e) {
            parties.save(e);
        }
    }

}
