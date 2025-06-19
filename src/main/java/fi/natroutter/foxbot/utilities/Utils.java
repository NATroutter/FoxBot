package fi.natroutter.foxbot.utilities;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;

import java.util.List;
import java.util.Map;

public class Utils {

    private static ConfigProvider config = FoxBot.getConfig();
    private static FoxLogger logger = FoxBot.getLogger();

    public static String randomFox() {return getFox(FoxLib.random(1,381));}
    public static String getFox(long num){
        int min = 1;
        int max = 381;
        if (!FoxLib.isBetween(num, min, max)) {
            logger.error("Invalid Fox index : "+num+" - range is "+min+" - "+max );
        }
        return ("https://cdn.nat.gg/projects/foxbot/foxes/" + num + ".jpg");
    }

    public static String cutString(String input, int maxLength) {
        return (input.length() > maxLength ? input.substring(0, maxLength) : input);
    }
    public static String cutStringEndDots(String input, int maxLength) {
        if (input.length() > maxLength) {
            return input.substring(0, maxLength-3)+"...";
        } else {
            return input;
        }
    }

}