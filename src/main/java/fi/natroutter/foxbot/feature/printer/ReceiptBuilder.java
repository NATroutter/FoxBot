package fi.natroutter.foxbot.feature.printer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the markup elements a FenPOS print job carries in its "data" array.
 * Every element is one line on the paper before wrapping.
 * <p>
 * Anything a user typed goes through {@link #escape(String)} first, so a message can never open a
 * tag or a {name} variable reference of its own.
 */
public class ReceiptBuilder {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private ReceiptBuilder() {}

    public static List<String> build(String sender, String message) {
        List<String> data = new ArrayList<>();
        data.add("<align=center><bold><size=2,2>FOXBOT MESSAGE</size></bold></align>");
        data.add("<hr>");
        data.add("<bold>From</bold><fill><bold>" + escape(sender) + "</bold>");
        data.add("<bold>At</bold><fill><bold>" + TIMESTAMP.format(LocalDateTime.now()) + "</bold>");
        data.add("<hr>");

        for (String line : message.split("\\R", -1)) {
            //an empty element is an empty line, <wrap></wrap> would be an empty tag around nothing
            data.add(line.isEmpty() ? "" : "<wrap>" + escape(line) + "</wrap>");
        }

        data.add("<feed=4>");
        data.add("<cut>");
        return data;
    }

    /**
     * Escapes the three characters the markup language reads as syntax. Ampersands go first, or the
     * escapes written for the other two would themselves be escaped again.
     */
    public static String escape(String input) {
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace("{", "&lbrace;");
    }

    /**
     * The API refuses control characters outright, so they are caught here to say why in Discord
     * instead. Line breaks are allowed through, {@link #build} turns them into separate elements.
     */
    public static boolean hasControlCharacters(String input) {
        return input.codePoints().anyMatch(cp -> cp != '\n' && cp != '\r' && Character.isISOControl(cp));
    }

}
