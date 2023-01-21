package fi.natroutter.foxbot.utilities;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class Utils {

    private static ConfigProvider config = FoxBot.getConfig();

    public static boolean validateConf(Object obj) {
        if (obj instanceof Integer) {
            int val = (Integer)obj;
            if (val > -1) {return true;}
            return false;
        }
        if (obj instanceof String) {
            String val = (String)obj;
            if (val != null && !val.equalsIgnoreCase("changeme")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static Color ThemeColor() {
        int r = config.get().getThemeColor().getRed();
        int g = config.get().getThemeColor().getGreen();
        int b = config.get().getThemeColor().getBlue();
        return new Color(r,g,b);
    }

    public static EmbedBuilder embedBase() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(ThemeColor());
        return eb;
    }

}
