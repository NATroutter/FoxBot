package fi.natroutter.foxbot.utilities;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.objects.GuildTime;
import fi.natroutter.foxbot.objects.HelpCommand;
import fi.natroutter.foxbot.objects.MinecraftData;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.codehaus.plexus.util.StringUtils;
import org.jsoup.Jsoup;

import java.awt.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {

    private static ConfigProvider config = FoxBot.getConfig();
    private static FoxLogger logger = FoxBot.getLogger();

    private static MarkdownSanitizer sanitizer = new MarkdownSanitizer();

    public static String stripMarkdown(String message) {
        if (message == null) {return "";}
        return sanitizer.withStrategy(MarkdownSanitizer.SanitizationStrategy.REMOVE).compute(message);
    }

    public static EmbedBuilder helpMessage(BaseCommand command, String title, HelpCommand... helpCommands) {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle(title);
        eb.setDescription("*Tässä on ohjeet kuinka hallita \""+command.getName()+"\" järjestelmää.*");
        for (HelpCommand hc : helpCommands) {
            if (hc.isRaw()) {
                eb.addField("⌨️ **/"+command.getName()+" "+hc.getArg()+"**","*"+hc.getDescription()+"*",false);
            } else {
                eb.addField("⌨️ **/"+command.getName()+" [Toiminto:" +hc.getArg()+ "]**","*"+hc.getDescription()+"*",false);
            }
        }
        return eb;
    }

    public static List<User> getUsersInVoice(Guild guild) {
        return guild.getVoiceChannels().stream()
                .flatMap(vc -> vc.getMembers().stream())
                .map(Member::getUser)
                .collect(Collectors.toList());
    }

    public static void sendPrivateMessage(User user, EmbedBuilder eb, String contentName) {
        user.openPrivateChannel().flatMap(pm -> pm.sendMessageEmbeds(eb.build())).queue((mm)->{
            logger.info("Sent private message to " + user.getGlobalName() + " ("+contentName+")");
        }, new ErrorHandler() .handle(ErrorResponse.CANNOT_SEND_TO_USER, (mm) -> {
            logger.info("Failed to send private message to " + user.getGlobalName() + " ("+contentName+")");
        }));
    }

    public static void removeMessages(MessageChannel channel, int amount) {
        List<Message> messages = channel.getHistory().retrievePast(amount).complete();
        for (Message message : messages) {
            if (message.isPinned()) {continue;}
            message.delete().complete();
        }
    }

    private static String randomString(int length) {
        String table = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int)(table.length() * Math.random());
            sb.append(table.charAt(index));
        }
        return sb.toString();
    }
    public static String createCode(int partCount, int partLenght) {
        List<String> parts = new ArrayList<>();
        for(int s = 0; s < partCount; s++) {
            parts.add(randomString(partLenght));
        }
        return String.join("-", parts);
    }

    public static MinecraftData getMinecraft(Object nameOrUUID, boolean isUUID) {
        String api = "https://api.mojang.com/users/profiles/minecraft/" + nameOrUUID;;
        if (isUUID) {
            api = "https://sessionserver.mojang.com/session/minecraft/profile/" + nameOrUUID;
        }
        try {
            String json = Jsoup.connect(api).ignoreContentType(true).userAgent("MotiBot/1.0").execute().body();
            return new Gson().fromJson(json, MinecraftData.class);
        } catch (Exception e) {
            logger.error("Failed to retrieve miencraft user data : Invalid name or uuid? >> " + nameOrUUID);
        }
        return new MinecraftData("Unknown", "Unknown");
    }

    public static EmbedBuilder errorEmbed(String msg) {
        return embedBase().setTitle("❗ "+msg+"");
    }

    public static EmbedBuilder error(String title, String msg) {
        EmbedBuilder eb = embedBase();
        eb.setTitle("❗ "+title+"");
        eb.setDescription(msg);
        return eb;
    }

    public static GuildTime getGuildTime(Member member) {
        if (member != null) {
            ZoneId helsinki = ZoneId.of("Europe/Helsinki");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            formatter.withZone(helsinki);

            LocalDateTime ldt = LocalDateTime.ofInstant(member.getTimeJoined().toInstant(), helsinki);
            long daysInGuild = Duration.between(ldt, LocalDateTime.now()).toDays();
            String joined = member.getTimeJoined().format(formatter);
            return new GuildTime(joined, daysInGuild);
        }
        return new GuildTime("Unknown", 0L);
    }

    public static String strictClean(String message, boolean reply) {
        if (StringUtils.isBlank(message)) {return "[Tyhjä kenttä!]";}
        message = message.replace("_", "");
        message = message.replace("-", "");
        message = message.replace("*", "");
        message = message.replace(">", "");
        message = message.replace("~", "");
        message = message.replace("`", "");
        if (reply) {
            if (message.length() < 1) {
                message = "[Vain viallisia merkkejä!]";
            }
        }
        return message;
    }


    public static boolean validateConf(Object obj) {
        if (obj instanceof Integer) {
            int val = (Integer)obj;
            if (val > -1) {return true;}
            return false;
        }
        if (obj instanceof String) {
            String val = (String)obj;
            if (val != null && !val.equalsIgnoreCase("changeme") && !val.equalsIgnoreCase("INSERT_TOKEN_HERE")) {
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

    public static int getRandom(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

}