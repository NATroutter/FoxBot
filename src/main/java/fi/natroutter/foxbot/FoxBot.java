package fi.natroutter.foxbot;

import fi.natroutter.foxbot.Database.MongoHandler;
import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.configs.Config;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxbot.utilities.NATLogger;
import lombok.Getter;

public class FoxBot {

    @Getter private static String VERSION = "1.0.0";

    @Getter private static ConfigProvider config;
    @Getter private static NATLogger logger;
    @Getter private static MongoHandler mongo;

    @Getter private static BotHandler bot;

    public static void main(String[] args) {
        logger = new NATLogger();
        config = new ConfigProvider();
        if (!config.isInitialized()) {return;}

        mongo = new MongoHandler();
        if (!mongo.isValidConfig()) {
            return;
        }

        bot = new BotHandler();

        //register new commands
        bot.registerCommand(new Clean());
        bot.registerCommand(new Permission());
        bot.registerCommand(new Batroutter());
        bot.registerCommand(new About());
        bot.registerCommand(new Statics());
        bot.registerCommand(new CoinFlip());
        bot.registerCommand(new Dice());
        //bot.registerCommand(new Vote());
        bot.registerCommand(new Yiff());
        bot.registerCommand(new Update());

        bot.connect(e->{
             if (!e) return;
            logger.info("Bot connected successfully!");
        });

    }


}
