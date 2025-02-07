package fi.natroutter.foxbot.database;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.controllers.GeneralController;
import fi.natroutter.foxbot.database.controllers.GroupController;
import fi.natroutter.foxbot.database.controllers.UserController;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;

@Getter
public class MongoHandler {

    private GeneralController general;
    private GroupController groups;
    private UserController users;

    private FoxLogger logger = FoxBot.getLogger();
    private ConfigProvider config = FoxBot.getConfig();

    private boolean initialized = false;

    public MongoHandler() {
        if (!config.isInitialized()) {
            logger.error("MongoDB Error : Configuration is not initialized!");
            return;
        }

        Config.MongoDB cfg = config.get().getMongoDB();

        if (cfg.getUri().equalsIgnoreCase("URI_HERE")) {
            logger.error("MongoDB Error : Database credentials are not configured!");
            return;
        }
        logger.info("Connecting to mongoDB...");

        MongoConnector connector = new MongoConnector(cfg);

        general = new GeneralController(connector);
        groups = new GroupController(connector);
        users = new UserController(connector);

        initialized = true;
        logger.info("MongoDB connection established!");
    }

    public void save(Object obj) {
        if (obj instanceof GroupEntry entry) {
            groups.save(entry);
        } else if (obj instanceof UserEntry entry) {
            users.save(entry);
        }
    }

}
