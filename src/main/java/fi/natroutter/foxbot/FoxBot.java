package fi.natroutter.foxbot;

import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxbot.handlers.GameRoles;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.Getter;

public class FoxBot extends FoxLib {

    /*
     * TODO
     * Add define esto juttu
     * Add link shortter
     */

    @Getter
    private static String ver = "1.0.6";

    @Getter
    private static ConfigProvider config;
    @Getter
    private static NATLogger logger;
    @Getter
    private static MongoHandler mongo;

    @Getter
    private static BotHandler bot;

    public static void main(String[] args) {
        logger = new NATLogger.Builder().setDebug(false).setPruneOlderThanDays(35).setSaveIntervalSeconds(300).build();

        printLine("\u001B[35m__________            ________      _____ \n" +
                "\u001B[35m___  ____/_________  ____  __ )_______  /_\n" +
                "\u001B[35m__  /_   _  __ \\_  |/_/_  __  |  __ \\  __/\n" +
                "\u001B[35m_  __/   / /_/ /_>  < _  /_/ // /_/ / /_  \n" +
                "\u001B[35m/_/      \\____//_/|_| /_____/ \\____/\\__/  \n" +
                "                                          ");
        printLine("\u001B[35m• Version: " + ver);
        printLine("\u001B[35m• Author: NATroutter");
        printLine("\u001B[35m• Website: https://NATroutter.fi");
        printLine(" ");

        logger.info("Starting FoxBot...");

        config = new ConfigProvider();
        if (!config.isInitialized()) {
            return;
        }

        mongo = new MongoHandler();
        if (!mongo.isValidConfig()) {
            return;
        }

        bot = new BotHandler();

        // register new commands
        bot.registerCommand(new Clean());
        bot.registerCommand(new Permission());
        bot.registerCommand(new Batroutter());
        bot.registerCommand(new About());
        bot.registerCommand(new Info());
        bot.registerCommand(new CoinFlip());
        bot.registerCommand(new Dice());
        bot.registerCommand(new Ask());
        bot.registerCommand(new Yiff());
        bot.registerCommand(new Update());
        bot.registerCommand(new Wakeup());
        bot.registerCommand(new Pick());

        bot.connect(e -> {
            if (!e) return;
            new GameRoles(bot);

            logger.info("Bot connected successfully!");
        });

        logger.info("Bot started!!!");
    }

}
