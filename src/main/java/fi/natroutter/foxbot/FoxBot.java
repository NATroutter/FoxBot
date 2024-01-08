package fi.natroutter.foxbot;

import fi.natroutter.foxbot.commands.*;
import fi.natroutter.foxbot.configs.CatifyProvider;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxbot.handlers.ConsoleClient;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.handlers.GameRoles;
import fi.natroutter.foxbot.listeners.EventLogger;
import fi.natroutter.foxbot.listeners.InviteTracker;
import fi.natroutter.foxbot.listeners.SocialListener;
import fi.natroutter.foxbot.listeners.SpamListener;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.restaction.RoleAction;

import java.awt.*;
import java.net.URL;
import java.util.List;

public class FoxBot extends FoxLib {

    /*
     * TODO
     * Add define esto juttu
     * Add link shortter
     * add userid to Join/quit
     * add some kind of lookup link to Join/quit
     */

    @Getter
    private static String ver = "1.0.8";

    @Getter
    private static ConfigProvider config;
    @Getter
    private static CatifyProvider catify;
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
        catify = new CatifyProvider();
        if (!catify.isInitialized()) {
            return;
        }

        mongo = new MongoHandler();
        if (!mongo.isValidConfig()) {
            return;
        }
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
        bot.registerCommand(new Update());
        bot.registerCommand(new Wakeup());
        bot.registerCommand(new Pick());
        bot.registerCommand(new Fox());
        bot.registerCommand(new SocialCredit());
        bot.registerCommand(new Invites());
        bot.registerCommand(new Catify());

        bot.connect(e -> {
            if (!e) return;

            new GameRoles(bot);
            createTraderRole();
            logger.info("Bot connected successfully!");

        });

        // register new listeners
        bot.registerListener(new EventLogger());
        bot.registerListener(new SocialListener());
        bot.registerListener(new SpamListener());
        bot.registerListener(new InviteTracker());

        logger.info("Bot started!!!");
        new ConsoleClient(bot);
    }

    public static boolean hasTraderRole(Member member) {
        Role trader = getTraderRole(member.getGuild());
        if (trader == null) return false;
        return member.getRoles().stream().filter(role -> role.getId().equals(trader.getId())).findFirst().orElse(null) != null;
    }
    public static Role getTraderRole(Guild guild) {
        List<Role> roles = guild.getRolesByName("Pokémon Trader", true);
        if (roles.isEmpty()) return null;
        return roles.get(0);
    }
    private static void createTraderRole() {
        bot.getJda().getGuilds().forEach(guild -> {
            String name = "Pokémon Trader";
            String icon = "https://cdn.nat.gg/img/discord/foxbot/Poke_Ball.png";
            List<Role> roles = guild.getRolesByName(name, true);
            Role trader = null;
            if (!roles.isEmpty()) {
                trader = roles.get(0);
            }
            if (trader == null) {
                RoleAction rAction = guild.createRole().setName(name).setHoisted(false).setMentionable(true);
                rAction.setColor(Color.decode("#f5d142"));
                if (guild.getBoostTier().equals(Guild.BoostTier.TIER_2) || guild.getBoostTier().equals(Guild.BoostTier.TIER_3)) {
                    try {
                        rAction = rAction.setIcon(Icon.from(new URL(icon).openStream()));
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
                rAction.queue();
            }
        });
    }

}
