package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfKeys;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.NATLogger;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import java.util.function.Consumer;

public class BotHandler {

    @Getter private boolean connected = false;
    @Getter private JDA jda;

    private JDABuilder builder;
    private CommandHandler commandHandler;

    private NATLogger logger = FoxBot.getLogger();
    private ConfigProvider config = FoxBot.getConfig();

    public BotHandler() {
        builder = JDABuilder.createDefault(config.getString(ConfKeys.TOKEN));
        builder.setActivity(Activity.watching("your behavior"));
        builder.setStatus(OnlineStatus.ONLINE);

        commandHandler = new CommandHandler(this);
        builder.addEventListeners(commandHandler);

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
