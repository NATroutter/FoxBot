package fi.natroutter.foxbot.feature.parties.listeners;

import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.feature.parties.data.PartyModals;
import fi.natroutter.foxbot.feature.parties.data.PartyChange;
import fi.natroutter.foxbot.feature.parties.data.RealRegion;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxframe.data.logs.LogUser;
import fi.natroutter.foxlib.cooldown.Cooldown;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogLevel;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
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
    private BotHandler bot = FoxBot.getBotHandler();

    private final Cooldown<String> cooldown_rename = createCooldown("party_rename");
    private final Cooldown<String> cooldown_edit = createCooldown("party_edit");
    private final Cooldown<String> cooldown_member_add = createCooldown("party_member_add");
    private final Cooldown<String> cooldown_member_remove = createCooldown("party_member_remove");
    private final Cooldown<String> cooldown_help = createCooldown("party_help");
    private final Cooldown<String> cooldown_visibility = createCooldown("party_visibility");
    private final Cooldown<String> cooldown_privacy = createCooldown("party_privacy");
    private final Cooldown<String> cooldown_nswf = createCooldown("party_nswf");

    private Cooldown<String> createCooldown(String actionName) {
        return new Cooldown.Builder<String>()
                .setDefaultCooldown(120)
                .setDefaultTimeUnit(TimeUnit.SECONDS)
                .onCooldownExpiry((userID,data)-> {
                    User user = null;
                    if (bot.isRunning()) {
                        user = bot.getJDA().getUserById(userID);
                    }
                    long seconds = TimeUnit.SECONDS.convert(data.cooldown().time(), data.cooldown().timeUnit());
                    logger.warn("Removed expired cooldown!",
                        new LogData("Action", actionName),
                        new LogUser(user),
                        new LogData("Duration", seconds + "s")
                    );
                })
                .build();
    }

    private void checkCooldown(IReplyCallback event, Cooldown<String> cooldown, Member member, String actionName, Runnable success) {
        permissions.has(member, event.getGuild(), Nodes.PARTY_VOICE_BYPASS_COOLDOWN, success, ()-> {
            if (cooldown.hasCooldown(member.getId())) {
                long remaining = cooldown.getCooldown(member.getId(), TimeUnit.SECONDS);
                partyHandler.error(event, "You're on Cooldown!","You need to wait "+ remaining +" more second(s) before performing this action again");
                logger.warn("Failed to perform action because user is no cooldown!",
                    new LogMember(member),
                    new LogData("Action", actionName),
                    new LogData("Remaining", remaining)
                );
                return;
            }
            cooldown.setCooldown(member.getId());
            success.run();
        });
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String id = event.getComponentId();
        MessageChannelUnion channel = event.getChannel();
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (member == null) return;

        switch (id.toLowerCase()) {
            case "party_channel_rename" -> {
                checkCooldown(event, cooldown_rename, member, "channel-rename", ()-> {
                    partyHandler.hasPermissions(channel, member, ()-> {
                        permissions.has(member, guild, Nodes.PARTY_VOICE_RENAME, ()->{

                            event.replyModal(PartyModals.renameChannel(member, channel.getName())).queue();
                            logger.info("Party channel rename modal opened!",
                                new LogMember(member),
                                new LogChannel(channel)
                            );

                        }, ()-> {
                            partyHandler.error(event, "Missing Permissions!","You don't have the permissions needed for renaming a party channel.");
                            logger.warn("Failed to rename party channel",
                                    new LogMember(member),
                                    new LogChannel(channel),
                                    new LogReason("No rename permissions")
                            );
                        });
                    }, (fatal,msg)-> {
                        partyHandler.error(event, "Failed to edit channel!", msg);
                        logger.log((fatal ? LogLevel.ERROR : LogLevel.WARN), "Failed to open party channel rename modal!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason(msg)
                        );
                    });
                });
            }
            case "party_channel_edit" -> {
                checkCooldown(event, cooldown_edit, member, "channel-edit", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        int userLimit = voice.getUserLimit();
                        int bitRate = voice.getBitrate()/1000;
                        RealRegion region = RealRegion.fromDiscord(voice.getRegion());
                        int slowMode = voice.getSlowmode();

                        event.replyModal(PartyModals.editChannel(userLimit,bitRate,region,slowMode)).queue();
                        logger.info("Party channel editor modal opened",
                                new LogMember(member),
                                new LogChannel(channel)
                        );

                    }, (fatal, msg)-> {
                        partyHandler.error(event, "Failed to edit channel!", msg);
                        logger.log((fatal ? LogLevel.ERROR : LogLevel.WARN), "Failed to open party channel editor modal!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason(msg)
                        );
                    });
                });
            }
            case "party_channel_member_add" -> {
                checkCooldown(event, cooldown_member_add, member, "channel-member-add", ()->{
                    mongo.getParties().findByChannelID(channel.getIdLong(), (party)-> {
                        if (party == null) {
                            logger.error("Failed to add member from party channel because the party does not exist!",
                                    new LogMember(member),
                                    new LogChannel(channel)
                            );
                            partyHandler.error(event, "Party Not Found!", "Could not add member because the party channel does not exist in the database.");
                            return;
                        }

                        if (!member.getId().equals(party.getOwnerID())) {
                            partyHandler.error(event, "Failed edit party members!", "Only party owner can edit members!");
                            logger.warn("Failed to open party member adding modal!",
                                    new LogMember(member),
                                    new LogChannel(channel),
                                    new LogReason("user isn't owner of the channel")
                            );
                            return;
                        }

                        event.replyModal(PartyModals.addMember()).queue();
                        logger.info("Party channel member adding modal opened!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );

                    });
                });
            }
            case "party_channel_member_remove" -> {
                checkCooldown(event, cooldown_member_remove, member, "channel-member-remove", ()->{
                    mongo.getParties().findByChannelID(channel.getIdLong(), (party)-> {
                        if (party == null) {
                            logger.error("Failed to remove member from party channel because the party does not exist!",
                                    new LogMember(member),
                                    new LogChannel(channel)
                            );
                            partyHandler.error(event, "Party Not Found!", "Could not remove member because the party channel does not exist in the database.");
                            return;
                        }

                        if (!member.getId().equals(party.getOwnerID())) {
                            partyHandler.error(event, "Failed edit party members!", "Only party owner can edit members!");
                            logger.warn("Failed to open party member removing modal!",
                                    new LogMember(member),
                                    new LogChannel(channel),
                                    new LogReason("user isn't owner of the channel")
                            );
                            return;
                        }

                        event.replyModal(PartyModals.removeMember()).queue();
                        logger.info("Party channel member removing modal opened!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );

                    });
                });
            }
            case "party_channel_member_kick" -> {
                checkCooldown(event, cooldown_member_remove, member, "channel-member-remove", ()->{
                    mongo.getParties().findByChannelID(channel.getIdLong(), (party)-> {
                        if (party == null) {
                            logger.error("Failed to kick member from party channel because the party does not exist!",
                                    new LogMember(member),
                                    new LogChannel(channel)
                            );
                            partyHandler.error(event, "Party Not Found!", "Could not kick member because the party channel does not exist in the database.");
                            return;
                        }

                        if (!member.getId().equals(party.getOwnerID())) {
                            partyHandler.error(event, "Failed to kick party members!", "Only the party owner can kick members!");
                            logger.warn("Failed to open party member kick modal!",
                                    new LogMember(member),
                                    new LogChannel(channel),
                                    new LogReason("user isn't owner of the channel")
                            );
                            return;
                        }

                        event.replyModal(PartyModals.kickMember()).queue();
                        logger.info("Party channel member kick modal opened!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );

                    });
                });
            }
            case "party_channel_help" -> {
                checkCooldown(event, cooldown_help, member, "channel-help", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        event.replyEmbeds(partyHandler.helpMessage()).setEphemeral(true).queue();
                        logger.info("Help message send!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );

                    }, (fatal, msg)-> {
                        partyHandler.error(event, "Failed to send help message!", msg);
                        logger.log((fatal ? LogLevel.ERROR : LogLevel.WARN), "Failed to send help message!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason(msg)
                        );
                    });
                });
            }
            case "party_channel_visibility" -> {
                checkCooldown(event, cooldown_visibility, member, "channel-visibility", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                            if (party == null) {
                                logger.error("Failed to change visibility on party channel because it does not exist!",
                                        new LogMember(member),
                                        new LogChannel(channel)
                                );
                                partyHandler.error(event, "Party Not Found!", "Could not change visibility because the party channel does not exist in the database.");
                                return;
                            }
                            boolean oldValue = party.isHidden();
                            boolean newValue = !oldValue;

                            party.setHidden(newValue);

                            partyHandler.setupEveryonePermissions(event.getGuild(), voice, newValue, (newValue ? false : party.isPublicAccess()), ()-> {
                                mongo.save(party);
                                partyHandler.updatePanel(guild, voice, party);

                                logger.info("Party channel setting has been updated!",
                                        new LogMember(member),
                                        new LogChannel(channel),
                                        new LogData("Visibility", (newValue ? "Hidden" : "Visible"))
                                );
                                partyHandler.success(event, "Channel Status Updated!", "Status has been successfully updated.",
                                        new PartyChange("Visibility", oldValue, newValue, "Hidden", "Visible")
                                );

                            });
                        });

                    }, (fatal, msg)-> {
                        partyHandler.error(event, "Failed to update visibility status!", msg);
                        logger.log((fatal ? LogLevel.ERROR : LogLevel.WARN), "Failed to change party channel visibility",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason(msg)
                        );
                    });
                });
            }
            case "party_channel_privacy" -> {
                checkCooldown(event, cooldown_privacy, member, "channel-privacy", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                            if (party == null) {
                                logger.error("Failed to change privacy on party channel because it does not exist!",
                                        new LogMember(member),
                                        new LogChannel(channel)
                                );
                                partyHandler.error(event, "Party Not Found!", "Could not change privacy because the party channel does not exist in the database.");
                                return;
                            }
                            boolean oldValue = party.isPublicAccess();
                            boolean newValue = !oldValue;

                            party.setPublicAccess(newValue);

                            partyHandler.setupEveryonePermissions(event.getGuild(), voice, party.isHidden(), newValue, ()-> {
                                mongo.save(party);
                                partyHandler.updatePanel(guild, voice, party);

                                logger.info("Party channel setting has been updated!",
                                        new LogMember(member),
                                        new LogChannel(channel),
                                        new LogData("Privacy", (newValue ? "Public" : "Private"))
                                );
                                partyHandler.success(event, "Channel Status Updated!", "Status has been successfully updated.",
                                        new PartyChange("Privacy", oldValue, newValue, "Public", "Private")
                                );
                            });
                        });

                    }, (fatal, msg)-> {
                        partyHandler.error(event, "Failed to update privacy status!", msg);
                        logger.log((fatal ? LogLevel.ERROR : LogLevel.WARN), "Failed to change party channel privacy!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason(msg)
                        );
                    });
                });
            }
            case "party_channel_nsfw" -> {
                checkCooldown(event, cooldown_nswf, member, "channel-nsfw", ()->{
                    partyHandler.hasPermissions(channel, member, ()-> {

                        VoiceChannel voice = channel.asVoiceChannel();
                        VoiceChannelManager manager = voice.getManager();

                        mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                            if (party == null) {
                                logger.error("Failed to change nswf on party channel because it does not exist!",
                                        new LogMember(member),
                                        new LogChannel(channel)
                                );
                                partyHandler.error(event, "Party Not Found!", "Could not change nswf because the party channel does not exist in the database.");
                                return;
                            }

                            boolean oldValue = voice.isNSFW();
                            boolean newValue = !oldValue;

                            manager.setNSFW(newValue).queue();

                            party.setNswf(newValue);
                            mongo.save(party);

                            partyHandler.updatePanel(guild, voice, party);

                            logger.info("Party channel setting has been updated!",
                                    new LogMember(member),
                                    new LogChannel(channel),
                                    new LogData("Nsfw", (newValue ? "Yes" : "No"))
                            );
                            partyHandler.success(event, "Channel Status Updated!", "Status has been successfully updated.",
                                    new PartyChange("NSWF", oldValue, newValue)
                            );

                        });

                    }, (fatal, msg)-> {
                        partyHandler.error(event, "Failed to update NSWF status!", msg);
                        logger.log((fatal ? LogLevel.ERROR : LogLevel.WARN), "Failed to change party channel nsfw status!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason(msg)
                        );
                    });
                });
            }
        }
    }
}
