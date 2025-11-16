package fi.natroutter.foxbot;

import fi.natroutter.foxbot.configs.AIRequestProvider;
import fi.natroutter.foxbot.configs.CatifyProvider;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.EmbedProvider;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.socialcredit.SocialCreditHandler;
import fi.natroutter.foxbot.feature.DailyFox;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.console.ConsoleClient;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;

public class FoxBot extends FoxLib {

    @Getter
    private static String ver = "1.2.0";

    @Getter
    private static ConfigProvider configProvider;
    @Getter
    private static CatifyProvider catifyProvider;
    @Getter
    private static EmbedProvider embedProvider;
    @Getter
    private static AIRequestProvider aiRequestProvider;
    @Getter
    private static FoxLogger logger;
    @Getter
    private static MongoHandler mongo;
    @Getter
    private static PermissionHandler permissionHandler;
    @Getter
    private static SocialCreditHandler socialCreditHandler;
    @Getter
    private static PartyHandler partyHandler;

    @Getter
    private static BotHandler botHandler;

    public static void main(String[] args) {

        logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("FoxBot")
                .build();

        println("\u001B[35m__________            ________      _____ \n" +
                "\u001B[35m___  ____/_________  ____  __ )_______  /_\n" +
                "\u001B[35m__  /_   _  __ \\_  |/_/_  __  |  __ \\  __/\n" +
                "\u001B[35m_  __/   / /_/ /_>  < _  /_/ // /_/ / /_  \n" +
                "\u001B[35m/_/      \\____//_/|_| /_____/ \\____/\\__/  \n" +
                "                                          ");
        println("\u001B[35m• Version: " + ver);
        println("\u001B[35m• Author: NATroutter");
        println("\u001B[35m• Website: https://NATroutter.fi");
        println(" ");

        logger.info("Starting FoxBot...");

        configProvider = new ConfigProvider();
        if (!configProvider.isInitialized()) {
            logger.error("ConfigProvider Failed to initialize!");
            return;
        }
        catifyProvider = new CatifyProvider();
        if (!catifyProvider.isInitialized()) {
            logger.error("CatifyProvider Failed to initialize!");
            return;
        }
        embedProvider = new EmbedProvider();
        if (!embedProvider.isInitialized()) {
            logger.error("EmbedProvider Failed to initialize!");
            return;
        }
        aiRequestProvider = new AIRequestProvider();
        if (!aiRequestProvider.isInitialized()) {
            logger.error("AIConfigProvider Failed to initialize!");
            return;
        }

        //Setup FoxFrame
        Config.Emojies emojies = configProvider.get().getEmojies();
        FoxFrame.setThemeColor(configProvider.get().getThemeColor().asColor());
        FoxFrame.setInfoEmoji(emojies.getInfo());
        FoxFrame.setErrorEmoji(emojies.getError());
        FoxFrame.setUsageEmoji(emojies.getUsage());
        FoxFrame.setLogger(logger);

        //Setup Database
        mongo = new MongoHandler();
        if (!mongo.isInitialized()) return;

        permissionHandler = new PermissionHandler();

        botHandler = new BotHandler();

        partyHandler = new PartyHandler();
        socialCreditHandler = new SocialCreditHandler();

        botHandler.whenConnected(jda -> {
            partyHandler.connected(jda);
            socialCreditHandler.connected(jda);
        });

        botHandler.connect();

        new DailyFox();
        new ConsoleClient(botHandler);
    }

}
