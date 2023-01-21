package fi.natroutter.foxbot.configs;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Config {

    private String token;
    private ThemeColor themeColor;
    private MongoDB mongoDB;


    @Getter @Setter
    public static class ThemeColor {
        private int red;
        private int green;
        private int blue;
    }

    @Getter @Setter
    public static class MongoDB {
        private String database;
        private String username;
        private String password;
        private String host;
        private int port;

    }

}