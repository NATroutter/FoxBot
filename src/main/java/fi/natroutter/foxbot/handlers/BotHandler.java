package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.NATLogger;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.function.Consumer;

public class BotHandler {

    @Getter private boolean connected = false;
    @Getter private JDA jda;

    private JDABuilder builder;
    private CommandHandler commandHandler;

    private NATLogger logger = FoxBot.getLogger();
    private ConfigProvider config = FoxBot.getConfig();

    public BotHandler() {
        builder = JDABuilder.create(config.get().getToken(),
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_BANS,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.GUILD_WEBHOOKS,
                GatewayIntent.GUILD_INVITES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_MESSAGE_TYPING,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGE_TYPING,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.SCHEDULED_EVENTS

        );
        builder.setActivity(Activity.watching("your behavior"));
        builder.setStatus(OnlineStatus.ONLINE);

        commandHandler = new CommandHandler(this);
        builder.addEventListeners(commandHandler);
        builder.setEnableShutdownHook(true);

    }

    public void registerCommand(BaseCommand command) {
        commandHandler.getCommands().add(command);
    }

    public void connect(Consumer<Boolean> consumer) {
        logger.info("Connecting to discord...");

        try {
            jda = builder.build();
            jda.awaitReady();
            connected = true;
            commandHandler.registerAll();
            consumer.accept(true);
        } catch (InterruptedException e) {
            logger.error("Interrupted : " + e.getMessage());
        }
        consumer.accept(false);
    }

    public void shutdown() {
        jda.shutdown();
        connected = false;
    }

}
