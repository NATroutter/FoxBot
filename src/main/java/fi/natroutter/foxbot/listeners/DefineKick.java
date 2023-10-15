package fi.natroutter.foxbot.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.BotHandler;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DefineKick {

    private BotHandler bot;

    private record DefineUser(Long guildID, Long userID, Long time) {}

    private FoxLogger logger = FoxBot.getLogger();

    // USE --> GuildVoiceSelfDeafenEvent

    public DefineKick(BotHandler bot) {
        this.bot = bot;

        new Timer().schedule(new TimerTask() {
            public void run() {
                loadDefined(defined -> {
                    if (defined != null && defined.size() > 0) {
                        for (Map.Entry<String, DefineUser> entry : defined.entrySet()) {
                            DefineUser def = entry.getValue();

                            Guild guild = bot.getJda().getGuildById(def.guildID());
                            if (guild == null) {continue;}
                            Member member = guild.getMemberById(def.userID());
                            if (member == null) {continue;}


                            if (member.getVoiceState() == null) {
                                defined.remove(entry.getKey());
                                continue;
                            }
                            if (!member.getVoiceState().isSelfDeafened() || member.getVoiceState().getChannel() == null) {
                                defined.remove(entry.getKey());
                                continue;
                            }
                            long mins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - def.time());
                            if (mins >= 2) {
                                logger.info("Kicking "+member.getUser().getGlobalName()+"("+member.getId()+") for being defined too long!");
                                member.kick().queue();
                            }
                        }
                    }
                });
            }
        }, 0, 60 * 1000L);
    }


    public void loadDefined(Consumer<ConcurrentHashMap<String, DefineUser>> defined) {
        ConcurrentHashMap<String, DefineUser> map = new ConcurrentHashMap<>();
        for(Guild guild : bot.getJda().getGuilds()) {
            for (VoiceChannel voice : guild.getVoiceChannels()) {
                if (voice.getMembers().size() < 1) {
                    continue;
                }
                for (Member member : voice.getMembers()) {
                    if (member.getVoiceState() == null) continue;
                    if (!member.getVoiceState().isSelfDeafened()) continue;
                    if (map.containsKey(member.getId())) continue;
                    map.put(member.getId(), new DefineUser(guild.getIdLong(), member.getIdLong(), System.currentTimeMillis()));
                }
            }
        }
        defined.accept(map);
    }

}
