package fi.natroutter.foxbot.feature.parties.listeners;

import fi.natroutter.foxbot.Cooldown;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class PartyButtonListener extends ListenerAdapter {

    private FoxLogger logger = FoxBot.getLogger();
    private PartyHandler partyHandler = FoxBot.getPartyHandler();
    private MongoHandler mongo = FoxBot.getMongo();
    private PermissionHandler permissions = FoxBot.getPermissionHandler();

    private final Cooldown<String> cooldown_rename = new Cooldown<>(15, TimeUnit.SECONDS);
    private final Cooldown<String> cooldown_edit = new Cooldown<>(15, TimeUnit.SECONDS);
    private final Cooldown<String> cooldown_help = new Cooldown<>(15, TimeUnit.SECONDS);
    private final Cooldown<String> cooldown_visibility = new Cooldown<>(15, TimeUnit.SECONDS);
    private final Cooldown<String> cooldown_privacy = new Cooldown<>(15, TimeUnit.SECONDS);
    private final Cooldown<String> cooldown_nswf = new Cooldown<>(15, TimeUnit.SECONDS);


    private void checkCooldown(IReplyCallback event, Cooldown<String> cooldown, Member member, String actionName, Runnable success) {
        permissions.has(member, event.getGuild(), Nodes.PARTY_VOICE_BYPASS_COOLDOWN, success, ()-> {
            if (cooldown.hasCooldown(member.getId())) {
                long remaining = cooldown.getCooldown(member.getId(), TimeUnit.SECONDS);
                partyHandler.error(event, "You're on Cooldown!","You need to wait "+ remaining +" more second(s) before performing this action again");
                logger.warn("Failed to perform action ("+actionName+") because user is no cooldown for next "+remaining+" second(s)");
                return;
            }
            cooldown.setCooldown(member.getId());
            success.run();
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getButton().getId();
        if (id == null) return;
        MessageChannelUnion channel = event.getChannel();
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (member == null) return;

        final String logName = FoxFrame.getLogName(member);
        final String logChannel = FoxFrame.getLogName(channel);

        switch (id.toLowerCase()) {
            case "party_channel_rename" -> {
                checkCooldown(event, cooldown_rename, member, "channel-rename", ()-> {
                    partyHandler.hasPermissions(channel, member, ()-> {
                        permissions.has(member, guild, Nodes.PARTY_VOICE_RENAME, ()->{

                            event.replyModal(partyHandler.renameModal(member, channel.getName())).queue();
                            logger.info("Party channel rename modal opened for "+ logName +" on channel " + logChannel);

                        }, ()-> {
                            partyHandler.error(event, "Missing Permissions!","You don't have the permissions needed for renaming a party channel.");
                            logger.error("Failed to rename party channel for "+ logName +" on channel " + logChannel + " Reason: No rename permissions!");
                        });
                    }, (msg)-> {
                        partyHandler.error(event, "Failed to edit channel!", msg);
                        logger.error("Failed to open party channel rename modal opened for "+ logName +" on channel " + logChannel + " Reason: " + msg);
                    });
                });
            }
            case "party_channel_edit" -> {
                checkCooldown(event, cooldown_edit, member, "channel-edit", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        int userLimit = voice.getUserLimit();
                        int bitRate = voice.getBitrate()/1000;
                        Region region = voice.getRegion();
                        int slowMode = voice.getSlowmode();

                        event.replyModal(partyHandler.editModal(userLimit,bitRate,region,slowMode)).queue();
                        logger.info("Party channel editor modal opened for "+ logName +" on channel " + logChannel);

                    }, (msg)-> {
                        partyHandler.error(event, "Failed to edit channel!", msg);
                        logger.error("Failed to open party channel editor modal opened for "+ logName +" on channel " + logChannel + " Reason: " + msg);
                    });
                });
            }
            case "party_channel_help" -> {
                checkCooldown(event, cooldown_help, member, "channel-help", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        event.replyEmbeds(partyHandler.helpMessage()).setEphemeral(true).queue();
                        logger.info("Help message send for "+logName+" on channel "+logChannel);

                    }, (msg)-> {
                        partyHandler.error(event, "Failed to send help message!", msg);
                        logger.error("Failed to send help message for "+ logName +" on channel " + logChannel + " Reason: " + msg);
                    });
                });
            }
            case "party_channel_visibility" -> {
                checkCooldown(event, cooldown_visibility, member, "channel-visibility", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                            if (party == null) {
                                logger.error("Failed to change visibility on party channel because it does not exist " + logChannel);
                                partyHandler.error(event, "Failed to update privacy status!", "Failed to retrieve party channel from database because it does not exists.");
                                return;
                            }
                            boolean hidden = !party.isHidden();

                            party.setHidden(hidden);
                            mongo.save(party);

                            partyHandler.setupEveryonePermissions(event.getGuild(), voice, hidden, (hidden ? false : party.isPublicAccess()), ()-> {
                                partyHandler.updatePanel(guild, voice);
                            });

                            logger.info("Party channel "+logChannel+" setting has been updated : visibility=" + (hidden ? "Hidden" : "Visible"));
                            partyHandler.success(event, "Channels visibility updated!", "► Status: " + (hidden ? "Hidden" : "Visible"));
                        });

                    }, (msg)-> {
                        partyHandler.error(event, "Failed to update visibility status!", msg);
                        logger.error("Failed to change party channel visibility for "+ logName +" on channel " + logChannel + " Reason: " + msg);
                    });
                });
            }
            case "party_channel_privacy" -> {
                checkCooldown(event, cooldown_privacy, member, "channel-privacy", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                            if (party == null) {
                                logger.error("Failed to change privacy on party channel because it does not exist " + logChannel);
                                partyHandler.error(event, "Failed to update privacy status!", "Failed to retrieve party channel from database because it does not exists!");
                                return;
                            }
                            boolean state = !party.isPublicAccess();

                            party.setPublicAccess(state);
                            mongo.save(party);

                            partyHandler.setupEveryonePermissions(event.getGuild(), voice, party.isHidden(), state, ()-> {
                                partyHandler.updatePanel(guild, voice);
                            });

                            logger.info("Party channel "+logChannel+" setting has been updated : privacy=" + (state ? "Public" : "Private"));
                            partyHandler.success(event, "Channels privacy updated!", "► Status: " + (state ? "Public" : "Private"));
                        });

                    }, (msg)-> {
                        partyHandler.error(event, "Failed to update privacy status!", msg);
                        logger.error("Failed to change party channel privacy for "+ logName +" on channel " + logChannel + " Reason: " + msg);
                    });
                });
            }
            case "party_channel_nsfw" -> {
                checkCooldown(event, cooldown_nswf, member, "channel-nsfw", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        VoiceChannelManager manager = voice.getManager();

                        mongo.getParties().findByChannelID(voice.getIdLong(), party-> {

                            boolean status = !voice.isNSFW();
                            manager.setNSFW(status).queue();

                            party.setNswf(status);
                            mongo.save(party);

                            partyHandler.updatePanel(guild, voice);

                            logger.info("Party channel "+logChannel+" setting has been updated : nsfw=" + (status ? "Yes" : "No"));
                            partyHandler.success(event, "Channels NSWF status updated!", "► Status: " + (status ? "Yes" : "No"));

                        });

                    }, (msg)-> {
                        partyHandler.error(event, "Failed to update NSWF status!", msg);
                        logger.error("Failed to change party channel nsfw status for "+ logName +" on channel " + logChannel + " Reason: " + msg);
                    });
                });
            }
        }
    }
}
