package fi.natroutter.foxbot.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Wakeup;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.UserEntry;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.handlers.permissions.Permissions;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class InviteTracker extends ListenerAdapter {

    private FoxLogger logger = FoxBot.getLogger();
    private MongoHandler mongo = FoxBot.getMongo();
    private ConfigProvider config = FoxBot.getConfig();

    private ConcurrentHashMap<String, ArrayList<Invite>> Invites = new ConcurrentHashMap<>();

    private static JDA jda() { return FoxBot.getBot().getJda(); }


    public InviteTracker() {
        for (Guild guild : jda().getGuilds()) {
            guild.retrieveInvites().queue(list ->Invites.put(guild.getId(), new ArrayList<>(list)));
        }
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                logger.warn("Checking user invite counts and updating roles!");
                for (Guild guild : jda().getGuilds()) {
                    for (Member member : guild.getMembers()) {
                        mongo.getUserByID(member.getId(), user->{

                            Role trader = FoxBot.getTraderRole(guild);
                            if (trader != null) {
                                mongo.getInviteCont(member.getId(), (count)->{
                                    if (count >= config.get().getGeneral().getInviteCountToRole()) {
                                        if (!FoxBot.hasTraderRole(member)) {
                                            guild.addRoleToMember(member, trader).queue();
                                            logger.info("Added trader role to " + member.getUser().getGlobalName() + " (Invite count: " + count + ")");
                                        }
                                    } else {
                                        Permissions.has(member, Node.BYPASS_TRADE_ROLE_REMOVE, has-> {
                                           if (!has) {
                                               if (FoxBot.hasTraderRole(member)) {
                                                   guild.removeRoleFromMember(member, trader).queue();
                                                   logger.info("Removed trader role from " + member.getUser().getGlobalName() + " (Invite count: " + count + ")");
                                               }
                                           }
                                        });
                                    }
                                });

                            }

                        });
                    }
                }
                logger.warn("User invite counts and roles updated!");
            }
        }, 0, 1000 * 60 * 60 * 12); // every 12h
    }

    @Override
    public void onGuildInviteCreate(GuildInviteCreateEvent e) {
        if (Invites.containsKey(e.getGuild().getId())) {
            ArrayList<Invite> invites = Invites.get(e.getGuild().getId());
            invites.add(e.getInvite());
            Invites.put(e.getGuild().getId(), invites);
        } else {
            ArrayList<Invite> list = new ArrayList<>();
            list.add(e.getInvite());
            Invites.put(e.getGuild().getId(), list);
        }
    }

//    @Override
//    public void onGuildInviteDelete(GuildInviteDeleteEvent e) {
//        if (Invites.containsKey(e.getGuild().getId())) {
//            ArrayList<Invite> invites = Invites.get(e.getGuild().getId());
//            invites.removeIf(inv -> inv.getCode().equalsIgnoreCase(e.getCode()));
//            Invites.put(e.getGuild().getId(), invites);
//        }
//    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent e) {
        if (e.getMember().getUser().isBot()) return;

        if (!Invites.containsKey(e.getGuild().getId())) return;
        List<Invite> invites = Invites.get(e.getGuild().getId());

        e.getGuild().retrieveInvites().queue(newInvs -> {
            for(Invite newInv : newInvs) {
                for(Invite oldInv : invites) {
                    if (newInv.getCode().equalsIgnoreCase(oldInv.getCode())) {

                        if (newInv.getInviter() == null) {
                            logger.info("User " + e.getMember().getUser().getGlobalName() + " joined with invite " + newInv.getCode() + " by unknown");
                            return;
                        }
                        logger.info("User " + e.getMember().getUser().getGlobalName() + " joined with invite " + newInv.getCode() + " by " + newInv.getInviter().getGlobalName());

                        mongo.getUserByID(e.getMember().getId(), (user)->{
                            user.setInvitedBy(newInv.getInviter().getId());
                            mongo.save(user);
                        });

                        return;

                    }
                }
            }
        });
        e.getGuild().retrieveInvites().queue(list ->Invites.put(e.getGuild().getId(), new ArrayList<>(list)));
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent e) {
        mongo.getUserByID(e.getUser().getId(), (user)-> {
            user.setInvitedBy("0");
            mongo.save(user);
        });
    }
}
