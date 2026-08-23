package fi.natroutter.foxbot.console;

import fi.natroutter.foxbot.console.commands.DebugCommand;
import fi.natroutter.foxframe.bot.DiscordBot;
import fi.natroutter.foxframe.console.ConsoleClient;
import fi.natroutter.foxframe.console.ConsoleCommand;

/**
 * The console client with FoxBot's own commands added.
 *
 * <p>{@link ConsoleClient} registers its built-in commands and then blocks in its input loop from
 * inside its own constructor, so there is no moment between construction and the first prompt at
 * which a caller could register anything. Overriding the loop is that moment.
 */
public class FoxConsoleClient extends ConsoleClient {

    /**
     * Static because the instance is not built yet: the parent constructor reaches {@link #loop()}
     * before any subclass field has been assigned.
     */
    private static final ConsoleCommand[] COMMANDS = {new DebugCommand()};

    public FoxConsoleClient(DiscordBot bot) {
        super(bot);
    }

    /** Registration is keyed by command name, so repeating it on later passes changes nothing. */
    @Override
    public void loop() {
        register(COMMANDS);
        super.loop();
    }
}
