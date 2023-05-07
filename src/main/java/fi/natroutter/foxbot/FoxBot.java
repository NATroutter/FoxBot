package fi.natroutter.foxbot;

import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxbot.handlers.GameRoles;
import fi.natroutter.foxbot.listeners.DefineKick;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.Getter;

public class FoxBot extends FoxLib {

    /*
        TODO
        Add event-logs to selected channel (join, leave, channelChange, etc)
        Add define esto juttu
        Add wakeup command that spams user channel to channel
     */


    @Getter private static String ver = "1.0.4";

    @Getter private static ConfigProvider config;
    @Getter private static NATLogger logger;
    @Getter private static MongoHandler mongo;

    @Getter private static BotHandler bot;

    public static void main(String[] args) {
        logger = new NATLogger.Builder().setDebug(false).setPruneOlderThanDays(35).setSaveIntervalSeconds(300).build();

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
            //DefineKick dk = new DefineKick(bot);
            new GameRoles(bot);

            logger.info("Bot connected successfully!");
        });

        logger.info("Bot started!!!");
    }


}
