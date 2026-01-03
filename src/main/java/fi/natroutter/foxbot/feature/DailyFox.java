package fi.natroutter.foxbot.feature;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.EmbedProvider;
import fi.natroutter.foxbot.configs.data.EmbedData;
import fi.natroutter.foxbot.configs.data.Placeholder;
import fi.natroutter.foxbot.data.Poems;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DailyFox {

    private MongoHandler mongo = FoxBot.getMongo();
    private ConfigProvider config = FoxBot.getConfigProvider();
    private EmbedProvider embed = FoxBot.getEmbedProvider();
    private FoxLogger logger = FoxBot.getLogger();
    private BotHandler bot = FoxBot.getBotHandler();

    private Duration durationSinceLastDailyFox = null;

    public DailyFox() {
        mongo.getGeneral().get(data->{
            long since = System.currentTimeMillis() - data.getLastDailyFox();
            durationSinceLastDailyFox = Duration.ofMillis(since);
        });

        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (!bot.isRunning()) return;

                mongo.getGeneral().get(data->{
                    JDA jda = bot.getJDA();

                    long since = System.currentTimeMillis() - data.getLastDailyFox();
                    Duration durationSinceLastDailyFox = Duration.ofMillis(since);

                    if (durationSinceLastDailyFox != null && durationSinceLastDailyFox.getSeconds() >= TimeUnit.SECONDS.convert(24, TimeUnit.HOURS)) {

                        TextChannel channel = jda.getTextChannelById(config.get().getChannels().getDailyFox());
                        if (channel == null) {
                            logger.error("[DailyFox] Invalid channel : Check your configuration!");
                            return;
                        }

                        if (data.getLastDailyFoxIndex() == 0) {data.setLastDailyFoxIndex(1);}
                        if (data.getLastDailyFoxIndex() >= 381) {data.setLastDailyFoxIndex(1);}
                        if (data.getLastPoemIndex() >= Poems.list.size()) {data.setLastPoemIndex(0);}

                        Poems.Poem poem = Poems.list.get(data.getLastPoemIndex());

                        EmbedData embedData = embed.get("daily_fox");
                        if (embedData == null) {
                            logger.error("[DailyFox] Embed data is invalid at 0x1!");
                            return;
                        }

                        MessageEmbed embedMsg = embedData.asEmbed(
                                new Placeholder("index",data.getTotalDailyFoxesSend()),
                                new Placeholder("title",poem.getTitle()),
                                new Placeholder("poem",poem.getContent()),
                                new Placeholder("image", Utils.getFox(data.getLastDailyFoxIndex()))
                        );

                        if (embed == null) {
                            logger.error("[DailyFox] Embed data is invalid at 0x2!");
                            return;
                        }

                        List<Message> messages = channel.getHistory().retrievePast(10).complete();
                        messages.forEach(msg-> {
                            logger.warn("Deleting old daily foxes", new LogChannel(channel));
                            msg.delete().complete();
                        });

                        channel.sendMessageEmbeds(embedMsg).queue();
                        logger.info("New daily fox has been send",
                                new LogChannel(channel),
                                new LogData("LastDailyFoxIndex", data.getLastDailyFoxIndex()),
                                new LogData("LastPoemIndex", data.getLastPoemIndex()),
                                new LogData("TotalDailyFoxesSend", data.getTotalDailyFoxesSend())
                        );

                        data.setLastDailyFox(System.currentTimeMillis());
                        data.setLastDailyFoxIndex(data.getLastDailyFoxIndex()+1);
                        data.setLastPoemIndex(data.getLastPoemIndex()+1);
                        data.setTotalDailyFoxesSend(data.getTotalDailyFoxesSend()+1);

                        mongo.getGeneral().save(data);

                    }
                });
            }
        }, 0, 1000 );//Every 30 Min   --- // every 12h --- }, 0, 1000 * 60 * 60 * 12);
    }

}
