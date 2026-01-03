package fi.natroutter.foxbot.configs.data;

import fi.natroutter.foxframe.data.CustomEmoji;
import fi.natroutter.foxlib.mongo.MongoConfig;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.List;

@Getter @Setter
public class Config {

    private String token;
    private ThemeColor themeColor;
    private List<OpenAI> openAI;
    private MongoConfig mongoDB;
    private ApiKeys apiKeys;
    private Zipline zipline;
    private Channels channels;
    private Party party;
    private General general;
    private SocialCredits socialCredits;
    private Emojies emojies;

    @Getter @Setter
    public static class ThemeColor {
        private int red;
        private int green;
        private int blue;

        public Color asColor() {
            return new Color(red,green,blue);
        }
    }

    @Getter @Setter
    public static class OpenAI {
        private String name;
        private String endpoint;
        private String apikey;
    }

    @Getter @Setter
    public static class ApiKeys {
        private String giphy;
    }

    @Getter @Setter
    public static class Zipline {
        private boolean enabled;
        private String endpoint;
        private String token;
    }

    @Getter @Setter
    public static class Channels {
        private String foodStash;
        private long instructionChannel;
        private long rulesChannel;
        private long rolesChannel;
        private long wakeup1;
        private long wakeup2;
        private long dailyFox;
    }

    @Getter @Setter
    public static class Party {
        private boolean enabled;
        private long newPartyChannel;
        private long partyCategory;
        private List<String> blacklistedNames;
    }

    @Getter @Setter
    public static class General {
        private int inviteCountToRole;
    }

    @Getter @Setter
    public static class SocialCredits {
        private int minVoiceTime;
        private int rewardInterval;
        private boolean useAllChannels;
        private List<Long> channels;
    }

    @Getter @Setter
    public static class Emojies {
        private CustomEmoji upvote;
        private CustomEmoji downvote;
        private CustomEmoji info;
        private CustomEmoji usage;
        private CustomEmoji error;
    }


}