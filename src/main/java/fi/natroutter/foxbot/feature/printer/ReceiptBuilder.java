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

    /**
     * @param message  the text to print, or null to print only an image
     * @param imageUrl an http(s) image for the API to fetch while the job compiles, or null for none
     * @param imageWidth the printed width as a percentage of the paper, clamped to the tag's 1-100
     */
    public static List<String> build(String sender, String message, String imageUrl, int imageWidth) {
        List<String> data = new ArrayList<>();
        data.add("<align=center><bold><size=2,2>FOXBOT MESSAGE</size></bold></align>");
        data.add("<hr>");
        data.add("<bold>From</bold><fill><bold>" + escape(sender) + "</bold>");
        data.add("<bold>At</bold><fill><bold>" + TIMESTAMP.format(LocalDateTime.now()) + "</bold>");
        data.add("<hr>");

        if (message != null) {
            for (String line : message.split("\\R", -1)) {
                //an empty element is an empty line, <wrap></wrap> would be an empty tag around nothing
                data.add(line.isEmpty() ? "" : "<wrap>" + escape(line) + "</wrap>");
            }
            data.add("<feed=1>");
        }

        if (imageUrl != null) {
            //a block admits no other tag and nothing else on its element, so the image owns this line
            data.add("<align=center><image=" + clampWidth(imageWidth) + ">" + imageUrl + "</image></align>");
        }

        data.add("<feed=4>");
        data.add("<cut>");
        return data;
    }

    /**
     * The tag takes 1-100 and refuses anything else, so a config left at 0 prints at full width
     * rather than failing the job over a value nobody typed.
     */
    public static int clampWidth(int imageWidth) {
        if (imageWidth < 1 || imageWidth > 100) return 100;
        return imageWidth;
    }

    /**
     * A URL is never escaped. The markup reads only {@code &lt;}, {@code &amp;} and {@code &lbrace;}
     * as entities and treats every other ampersand as literal text, so a query string survives as
     * written, while escaping it would corrupt the URL if block content is taken literally.
     * <p>
     * What has to hold instead is that the URL carries nothing the parser could read as markup. A
     * Discord attachment URL never does, this is here so that stays true of whatever supplies one next.
     */
    public static boolean isSafeUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false;
        return url.codePoints().noneMatch(cp -> cp == '<' || cp == '>' || cp == '{' || Character.isWhitespace(cp));
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
