package fi.natroutter.foxbot.utilities;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;

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
}