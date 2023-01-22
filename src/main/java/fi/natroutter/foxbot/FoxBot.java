package fi.natroutter.foxbot;

import fi.natroutter.foxbot.Database.MongoHandler;
import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.Getter;

public class FoxBot extends FoxLib {

    @Getter private static String ver = "1.0.3";

    @Getter private static ConfigProvider config;
    @Getter private static NATLogger logger;
    @Getter private static MongoHandler mongo;

    @Getter private static BotHandler bot;

    public static void main(String[] args) {
        logger = new NATLogger();

        printLine("\u001B[35m__________            ________      _____ \n" +
                "___  ____/_________  ____  __ )_______  /_\n" +
                "__  /_   _  __ \\_  |/_/_  __  |  __ \\  __/\n" +
                "_  __/   / /_/ /_>  < _  /_/ // /_/ / /_  \n" +
                "/_/      \\____//_/|_| /_____/ \\____/\\__/  \n" +
                "                                          ");
        printLine("\u001B[35m• Version: " + ver);
        printLine("\u001B[35m• Author: NATroutter");
        printLine("\u001B[35m• Website: https://NATroutter.fi");
        printLine(" ");

        logger.info("Starting FoxBot...");

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

        logger.info("Bot started!!!");
    }


}
