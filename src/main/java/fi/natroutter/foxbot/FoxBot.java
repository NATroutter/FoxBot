package fi.natroutter.foxbot;

import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.configs.CatifyProvider;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.EmbedProvider;
import fi.natroutter.foxbot.data.Poems;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxbot.handlers.ConsoleClient;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.handlers.DailyFoxHandler;
import fi.natroutter.foxbot.listeners.EventLogger;
import fi.natroutter.foxbot.listeners.InviteTracker;
import fi.natroutter.foxbot.listeners.SocialListener;
import fi.natroutter.foxbot.listeners.SpamListener;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FoxBot extends FoxLib {

    /*
     * TODO
     * Add /docs komento jolla saa haettua plugin/mod documentations
     * Check that all commands has permissions
     * Add define esto juttu
     * Add link shortter
     * add userid to Join/quit
     * add some kind of lookup link to Join/quit
     * Add party system /party create/rename/delete etc...
     *   - idea is to allow temp channels
     *   - create gategory where are empty channel "+ New party" when user joins it creates new channel and moves user to that also saves who owns the party and allow party owner to edit party with commands
     */

    @Getter
    private static String ver = "1.0.12";
    @Getter

    private static ConfigProvider config;
    @Getter
    private static CatifyProvider catify;
    @Getter
    private static EmbedProvider embeds;
    @Getter
    private static FoxLogger logger;
    @Getter
    private static MongoHandler mongo;
    @Getter
    private static CreditHandler creditHandler;

    @Getter
    private static BotHandler bot;

    public static void main(String[] args) {

        logger = new FoxLogger.Builder().setDebug(false).setPruneOlderThanDays(35).setSaveIntervalSeconds(300).build();

        printLn("\u001B[35m__________            ________      _____ \n" +
                "\u001B[35m___  ____/_________  ____  __ )_______  /_\n" +
                "\u001B[35m__  /_   _  __ \\_  |/_/_  __  |  __ \\  __/\n" +
                "\u001B[35m_  __/   / /_/ /_>  < _  /_/ // /_/ / /_  \n" +
                "\u001B[35m/_/      \\____//_/|_| /_____/ \\____/\\__/  \n" +
                "                                          ");
        printLn("\u001B[35m• Version: " + ver);
        printLn("\u001B[35m• Author: NATroutter");
        printLn("\u001B[35m• Website: https://NATroutter.fi");
        printLn(" ");

        logger.info("Starting FoxBot...");

        config = new ConfigProvider();
        if (!config.isInitialized()) {
            logger.error("ConfigProvider Failed to initialize!");
            return;
        }
        catify = new CatifyProvider();
        if (!catify.isInitialized()) {
            logger.error("CatifyProvider Failed to initialize!");
            return;
        }
        embeds = new EmbedProvider();
        if (!embeds.isInitialized()) {
            logger.error("EmbedProvider Failed to initialize!");
            return;
        }

        mongo = new MongoHandler();
        creditHandler = new CreditHandler();

        bot = new BotHandler();

        // register new commands
        bot.registerCommand(new Prune());
        bot.registerCommand(new Permission());
        bot.registerCommand(new Batroutter());
        bot.registerCommand(new About());
        bot.registerCommand(new Info());
        bot.registerCommand(new CoinFlip());
        bot.registerCommand(new Dice());
        bot.registerCommand(new Ask());
        bot.registerCommand(new Yiff());
        bot.registerCommand(new Embed());
        bot.registerCommand(new Wakeup());
        bot.registerCommand(new Pick());
        bot.registerCommand(new Fox());
        bot.registerCommand(new SocialCredit());
        bot.registerCommand(new Invites());
        bot.registerCommand(new Catify());

        bot.connect(jda -> {
            if (jda != null) {
                // register new listeners
                jda.addEventListener(new EventLogger());
                jda.addEventListener(new SocialListener());
                jda.addEventListener(new SpamListener());
                jda.addEventListener(new InviteTracker());

                logger.info("Bot connected successfully!");
                logger.info("Bot started!!!");
            }
        });

        new DailyFoxHandler();

        new ConsoleClient(bot);
    }

}
