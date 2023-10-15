package fi.natroutter.foxbot.configs;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Config {

    private String token;
    private ThemeColor themeColor;
    private MongoDB mongoDB;
    private ApiKeys apiKeys;
    private channels channels;
    private general general;

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

    @Getter @Setter
    public static class ApiKeys {
        private String giphy;
    }

    @Getter @Setter
    public static class channels {
        private String foodStash;
        private int minVoiceTime;
        private int rewardInterval;
        private long instructionChannel;
        private long rulesChannel;
        private long rolesChannel;
    }

    @Getter @Setter
    public static class general {
        private int inviteCountToRole;
    }

}