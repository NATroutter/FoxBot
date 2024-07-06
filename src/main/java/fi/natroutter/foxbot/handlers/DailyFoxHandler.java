package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.EmbedProvider;
import fi.natroutter.foxbot.configs.data.EmbedData;
import fi.natroutter.foxbot.configs.data.Placeholder;
import fi.natroutter.foxbot.data.Poems;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

public class DailyFoxHandler {

    private MongoHandler mongo = FoxBot.getMongo();
    private ConfigProvider config = FoxBot.getConfig();
    private EmbedProvider embed = FoxBot.getEmbeds();
    private FoxLogger logger = FoxBot.getLogger();
    private BotHandler bot = FoxBot.getBot();

    public DailyFoxHandler() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                mongo.getGeneral().get(data->{
                    long since = System.currentTimeMillis() - data.getLastDailyFox();
                    Duration dura = Duration.ofMillis(since);
                    if (dura.getSeconds() >= (60*60*24)) {
                        if (bot == null) {
                            logger.error("[DailyFox] Invalid Connectio 0x1 : Check your configuration!");
                            return;
                        }
                        JDA jda = bot.getJda();
                        if (jda == null) {
                            logger.error("[DailyFox] Invalid Connection 0x2 : Check your configuration!");
                            return;
                        }

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

                        channel.sendMessageEmbeds(embedMsg).queue();

                        data.setLastDailyFox(System.currentTimeMillis());
                        data.setLastDailyFoxIndex(data.getLastDailyFoxIndex()+1);
                        data.setLastPoemIndex(data.getLastPoemIndex()+1);
                        data.setTotalDailyFoxesSend(data.getTotalDailyFoxesSend()+1);
                    }
                    mongo.getGeneral().save(data);
                });
            }
        }, 0, 1000 * 60 * 30 );//Every 30 Min   --- // every 12h --- }, 0, 1000 * 60 * 60 * 12);
    }

}
