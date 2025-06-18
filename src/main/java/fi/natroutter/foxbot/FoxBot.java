package fi.natroutter.foxbot;

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

    /*
     * TODO
     *
     * !!! move the Cooldown class to foxLib
     * !!! update the DiscordCommand class (command handler) to use the cooldown class
     * !!! update the onCooldownRemoved consumer in DiscordCommand class to display user name (make message better)
     * !!! add log message when cooldown is removed in button cooldown similar that in DiscordCommand class
     *
     *
     * IMPROVE OLD COMMANDS WHERE replies are send in the end of the command only and title and description is set in swiches etc see prmissions command its trash....
     * auto complete for commands
     * Add /docs komento jolla saa haettua plugin/mod documentations
     * Pingaus daily fox kanavalle kustom roolille (mahollisuus lisätä viesti embediin???)
     * Wakeup komento on rikki
     * Embed systeemis ei toimi aika timestamp/footer thing "updated | aika?"
     * tarkista kaikki komennot et onko niis järkevät limitit public usageeen (perissions / cooldowns)
     * tarkista kaikkien roolien permit ja laita ne kuntoon
     * kun click embed viestiä drop down menu jostai voi valita export as base64
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
    private static String ver = "1.2.0";

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

        //Setup FoxFrame
        Config.Emojies emojies = config.get().getEmojies();
        FoxFrame.setThemeColor(config.get().getThemeColor().asColor());
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

        botHandler.whenConnected(jda -> {
            partyHandler.connected(jda);
            socialCreditHandler = new SocialCreditHandler(jda);
        });

        botHandler.connect();

        new DailyFox();
        new ConsoleClient(botHandler);
    }

}
