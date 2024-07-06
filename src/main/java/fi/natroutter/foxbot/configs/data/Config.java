package fi.natroutter.foxbot.configs.data;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.List;

@Getter @Setter
public class Config {

    private String token;
    private ThemeColor themeColor;
    private MongoDB mongoDB;
    private ApiKeys apiKeys;
    private Channels channels;
    private General general;
    private SocialCredits socialCredits;
    private Emojies emojies;

    @Getter @Setter
    public static class ThemeColor {
        private int red;
        private int green;
        private int blue;
    }

    @Getter @Setter
    public static class MongoDB {
        private String uri;
        private String database;

    }

    @Getter @Setter
    public static class ApiKeys {
        private String giphy;
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
        private EmojiData upvote;
        private EmojiData downvote;
        private EmojiData info;
        private EmojiData usage;
        private EmojiData error;
    }

    @Getter @Setter
    public static class EmojiData {
        private String name;
        private long id;
        private boolean animated;

        public Emoji asEmoji(){
            return Emoji.fromCustom(name,id,animated);
        }

        public String asFormat() {return asEmoji().getFormatted();}
    }

}