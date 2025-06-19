package fi.natroutter.foxbot.feature.parties.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.PartyEntry;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.feature.parties.data.SettingChange;
import fi.natroutter.foxbot.feature.parties.data.SlowMode;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogError;
import fi.natroutter.foxlib.logger.types.LogReason;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class PartyModalListener extends ListenerAdapter {

    private FoxLogger logger = FoxBot.getLogger();
    private PartyHandler partyHandler = FoxBot.getPartyHandler();
    private MongoHandler mongo = FoxBot.getMongo();


    private Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private Region tryParseRegion(String value) {
        return switch (value) {
            case "automatic" -> Region.AUTOMATIC;
            case "brazil" -> Region.BRAZIL;
            case "hongkong" -> Region.HONG_KONG;
            case "india" -> Region.INDIA;
            case "japan" -> Region.JAPAN;
            case "rotterdam" -> Region.ROTTERDAM;
            case "singapore" -> Region.SINGAPORE;
            case "south_africa" -> Region.SOUTH_AFRICA;
            case "sydney" -> Region.SYDNEY;
            case "us-central" -> Region.US_CENTRAL;
            case "us-east" -> Region.US_EAST;
            case "us-south" -> Region.US_SOUTH;
            case "us-west" -> Region.US_WEST;
            default -> null;
        };
    }

    private boolean isValidSnowflake(String id) {
        if (id == null) return false;
        try {
            Long.parseUnsignedLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Member findMember(Guild guild, String idOrName) {
        if (isValidSnowflake(idOrName)) {
            return guild.getMemberById(idOrName);
        }
        return guild.getMembersByName(idOrName, true).stream().findFirst().orElse(null);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        MessageChannelUnion channel = event.getChannel();
        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (guild == null) return;

        ModalInteraction interaction = event.getInteraction();

        switch (id.toLowerCase()) {
            case "party_channel_edit_modal" -> {
                Integer userLimit = Optional.ofNullable(interaction.getValue("user_limit")).map(ModalMapping::getAsString).map(this::tryParseInt).orElse(null);
                Integer bitRate = Optional.ofNullable(interaction.getValue("bitrate")).map(ModalMapping::getAsString).map(this::tryParseInt).orElse(null);
                Region region = Optional.ofNullable(interaction.getValue("region")).map(ModalMapping::getAsString).map(this::tryParseRegion).orElse(null);
                String slowModeOPT = Optional.ofNullable(interaction.getValue("slowmode")).map(ModalMapping::getAsString).orElse(null);

                //Validations for userLimit
                if (userLimit == null) {
                    partyHandler.error(event, "User Limit Must Be a Number!", "Please enter a valid number");
                    logger.warn("Failed to edit party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("user limit is not a number")
                    );
                    return;
                }

                if (!FoxLib.isBetween(userLimit, 0,99)) {
                    partyHandler.error(event, "Invalid User Limit Range!", "The user limit must be a number between 0 and 99 inclusive.");
                    logger.warn("Failed to edit party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("user limit is not between 0 and 99")
                    );
                    return;
                }

                //Validations for bitRate
                if (bitRate == null) {
                    partyHandler.error(event, "Bitrate Must Be a Number!", "Please enter a valid number.");
                    logger.warn("Failed to edit party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("bitrate is not a number")
                    );
                    return;
                }

                if (!FoxLib.isBetween(bitRate, 8,96)) {
                    partyHandler.error(event, "Invalid Bitrate Range!", "The bitrate must be a number between 8 and 96 inclusive.");
                    logger.warn("Failed to edit party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("bitrate is not between 8 and 96")
                    );
                    return;
                }

                //Validations for region
                if (region == null) {
                    partyHandler.error(event, "Unsupported Region!", "The region you selected is not allowed. Please select a valid region.\n\nRegions:\n``automatic, brazil, hongkong, india, japan, rotterdam, singapore, south_africa, sydney, us-central, us-east, us-south, us-west``");
                    logger.warn("Failed to edit party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Invalid region")
                    );
                    return;
                }

                //Validations for region
                if (slowModeOPT == null) {
                    String validModes = Arrays.stream(SlowMode.values()).map(SlowMode::getArg).collect(Collectors.joining(", "));
                    logger.warn("Failed to edit party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Invalid slowmode")
                    );
                    partyHandler.error(event, "Unsupported Slowmode!", "The slowmode you selected is not allowed. Please select a valid slowmode.\n\nSlow modes:\n``"+ validModes  +"``");
                    return;
                }
                SlowMode sMode = SlowMode.fromArg(slowModeOPT);

                VoiceChannel voice = channel.asVoiceChannel();

                //Save selected values to database and update panel etc
                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to edit party channel because the party does not exist!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );
                        partyHandler.error(event, "Party Not Found!", "Could not change settings because the party channel does not exist in the database.");
                        return;
                    }

                    int oldUserLimit = voice.getUserLimit();
                    int oldBitRate = voice.getBitrate() / 1000;
                    Region oldRegion = voice.getRegion();
                    int oldSlowMode = voice.getSlowmode();

                    if (userLimit == oldUserLimit && bitRate == oldBitRate && region.equals(oldRegion) && sMode.getValue() == oldSlowMode) {
                        partyHandler.warn(event, "No Changes Detected!", "The settings you provided are the same as the current settings. No changes were made.");
                        logger.warn("Failed to edit party channel!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogData("UserLimit", oldUserLimit+"->"+userLimit),
                                new LogData("Bitrate", oldBitRate+"->"+bitRate),
                                new LogData("Region", oldRegion.getKey()+"->"+region.getKey()),
                                new LogData("SlowMode", SlowMode.fromValue(oldSlowMode).getArg()+"->"+sMode.getArg()),
                                new LogReason("No changes detected")
                        );
                        return;
                    }

                    party.setUserLimit(userLimit);
                    party.setBitRate(bitRate);
                    party.setRegion(region.getKey());
                    party.setSlowMode(sMode.getValue());

                    String oldSlow = SlowMode.fromValue(oldSlowMode) != null ? SlowMode.fromValue(oldSlowMode).getArg() : String.valueOf(oldSlowMode);

                    voice.getManager()
                            .setUserLimit(userLimit)
                            .setBitrate(bitRate * 1000)
                            .setRegion(region)
                            .setSlowmode(sMode.getValue())
                            .queue(s-> {
                                mongo.save(party);
                                partyHandler.updatePanel(guild, voice, party);

                                partyHandler.success(event, "Channel Settings Updated!", "Settings has been successfully updated.",
                                            new SettingChange("UserLimit", oldUserLimit, userLimit),
                                            new SettingChange("Bitrate", oldBitRate, bitRate),
                                            new SettingChange("Region", oldRegion.getKey(), region.getKey()),
                                            new SettingChange("SlowMode", oldSlow, sMode.getArg())
                                        );

                                logger.info("Party channel settings has been updated",
                                        new LogMember(member),
                                        new LogChannel(channel),
                                        new LogData("UserLimit", oldUserLimit+"->"+userLimit) ,
                                        new LogData("Bitrate", oldBitRate+"->"+bitRate),
                                        new LogData("Region", oldRegion.getKey()+"->"+region.getKey()),
                                        new LogData("SlowMode", oldSlow+"->"+sMode.getArg())
                                );
                            }, error -> {
                                logger.error("Failed to edit party channel!",
                                    new LogMember(member),
                                    new LogChannel(channel),
                                    new LogData("UserLimit", oldUserLimit+"->"+userLimit) ,
                                    new LogData("Bitrate", oldBitRate+"->"+bitRate),
                                    new LogData("Region", oldRegion.getKey()+"->"+region.getKey()),
                                    new LogData("SlowMode", oldSlow+"->"+sMode.getArg()),
                                    new LogError(error)
                                );
                                partyHandler.error(event, "Failed to Edit Channel!", error.getMessage());
                            });
                });

            }
            case "party_channel_rename_modal" -> {
                VoiceChannel voice = channel.asVoiceChannel();
                String oldName = voice.getName();

                String newName = Optional.ofNullable(interaction.getValue("new_name")).map(ModalMapping::getAsString).orElse("");

                if (newName.isBlank()) {
                    partyHandler.warn(event, "Name Not Provided!", "You didn’t provide a new name. The current name will remain unchanged.");
                    logger.warn("Failed to rename party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogData("OldName", oldName),
                            new LogReason("No name provided")
                    );
                    return;
                }

                if (partyHandler.isBlacklisted(newName)) {
                    partyHandler.error(event, "Inappropriate Name!", "The provided name contains words that are not allowed. Please choose a different name.");
                    logger.warn("Failed to rename party channel",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogData("OldName", oldName),
                            new LogData("NewName", newName),
                            new LogReason("Name contains blacklisted or inappropriate words")
                    );
                    return;
                }

                if (newName.length() > 25) {
                    partyHandler.error(event, "Too Long name!", "Name can not be longer than 25 characters.");
                    logger.warn("Failed to rename party channel",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogData("OldName", oldName),
                            new LogData("NewName", newName.substring(0, 25)+"..."),
                            new LogReason("Name is longer than 25 characters")
                    );
                    return;
                }

                if (!isSafeName(newName)) {
                    partyHandler.error(event, "Name contains illegal characters!", "The name contains one or more illegal characters.\nPlease use only letters, numbers, and the following symbols:\n``- _ . , + * ! # % & / = ? \\ ( ) { } [ ] @ £ $ € ^ ~ < > |``");
                    logger.warn("Failed to rename party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogData("OldName", oldName),
                            new LogData("NewName", newName),
                            new LogReason("name contains illegal characters")
                    );
                    return;
                }

                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to rename party channel because the party does not exist!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogData("OldName", oldName),
                                new LogData("NewName", newName)
                        );
                        partyHandler.error(event, "Party Not Found!", "Could not rename channel because the party channel does not exist in the database.");
                        return;
                    }

                    if (party.getName().equalsIgnoreCase(newName)) {
                        partyHandler.warn(event, "No Change Detected!", "The new name is the same as the current name. No changes were made.");
                        logger.warn("Failed to rename party channel!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogData("OldName", oldName),
                                new LogData("NewName", newName),
                                new LogReason("New name is the same as old name")
                        );
                        return;
                    }

                    party.setName(newName);

                    voice.getManager().setName(newName).queue(s->{
                        partyHandler.updatePanel(guild, voice, party);
                        mongo.save(party);

                        partyHandler.success(event, "Channel Name Updated!", "The channel name has been successfully updated.",
                                new SettingChange("Name", oldName, newName)
                        );
                        logger.info("Party channel has been renamed!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogData("OldName", oldName),
                                new LogData("NewName", newName)
                        );

                    }, error-> {
                        logger.error("Failed to rename party channel!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogData("OldName", oldName),
                            new LogData("NewName", newName),
                            new LogError(error)
                        );
                        partyHandler.error(event, "Failed to Rename Channel!", error.getMessage());
                    });
                });
            }
            case "party_channel_member_add"-> {
                String memberId = Optional.ofNullable(interaction.getValue("member_id")).map(ModalMapping::getAsString).orElse("");
                String memberAdmin = Optional.ofNullable(interaction.getValue("member_admin")).map(ModalMapping::getAsString).orElse("");

                if (memberId.isBlank()) {
                    logger.warn("Failed to update party members!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Member field is blank")
                    );
                    partyHandler.error(event, "Missing Member Information!", "Please provide a member ID or name before continuing.");
                    return;
                }
                if (memberAdmin.isBlank()) {
                    logger.warn("Failed to update party members!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Admin field is blank")
                    );
                    partyHandler.error(event, "Missing Admin Status!", "You didn't provide an admin status. Please enter a valid value.");
                    return;
                }
                if (!memberAdmin.equalsIgnoreCase("YES") && !memberAdmin.equalsIgnoreCase("NO")) {
                    logger.warn("Failed to update party members!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Admin field value was invalid"),
                            new LogData("AdminStatus", memberAdmin)
                    );
                    partyHandler.error(event, "Invalid Admin Status!", "Admin status must be either 'YES' or 'NO'. Please provide a valid value.");
                    return;
                }
                boolean admin = memberAdmin.equalsIgnoreCase("YES");

                VoiceChannel voice = channel.asVoiceChannel();

                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to add member from party channel because the party does not exist!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );
                        partyHandler.error(event, "Party Not Found!", "Could not add member because the party channel does not exist in the database.");
                        return;
                    }

                    Member target = findMember(guild,memberId);
                    if (target == null) {
                        logger.warn("Failed to update party members!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason("Target member not found"),
                                new LogData("MemberID", memberId),
                                new LogData("AdminStatus", memberAdmin)
                        );
                        partyHandler.error(event, "Member Not Found", "Could not find the specified member. They might have left the server or the ID or name is invalid.");
                        return;
                    }

                    if (target.getId().equalsIgnoreCase(party.getOwnerID())) {
                        logger.warn("Party member update blocked.",
                                new LogMember(member),
                                new LogMember("Target", target),
                                new LogChannel(channel),
                                new LogData("MemberID", memberId),
                                new LogData("AdminStatus", memberAdmin),
                                new LogReason("Attempted to add the party owner as a regular member")
                        );
                        partyHandler.error(event, "Invalid Member", "The party owner is already part of the party and cannot be added as a regular member.");
                        return;
                    }


                    party.getMembers().add(new PartyEntry.PartyMember(target.getIdLong(), target.getUser().getName(), admin));

                    partyHandler.setupPermissions(voice, target, admin, ()->{
                        mongo.save(party);
                        partyHandler.updatePanel(guild, voice, party);

                        partyHandler.successHidden(event, "Channel Members Updated!", "The channel members has been successfully updated.");
                        logger.info("Party channel members has been updated!",
                                new LogMember(member),
                                new LogMember("Target", target),
                                new LogChannel(channel),
                                new LogData("MemberID", memberId),
                                new LogData("AdminStatus", memberAdmin),
                                new LogData("AddedMember", new LogMember(target))
                        );
                    }, (error) -> {
                        logger.error("Failed to update party channel members!",
                                new LogMember(member),
                                new LogMember("Target", target),
                                new LogChannel(channel),
                                new LogData("MemberID", memberId),
                                new LogData("AdminStatus", memberAdmin),
                                new LogError(error)
                        );
                        partyHandler.error(event, "Failed to Update Members!", "Failed to setup permissions for the member.");
                    });
                });
            }
            case "party_channel_member_remove"-> {
                String memberId = Optional.ofNullable(interaction.getValue("member_id")).map(ModalMapping::getAsString).orElse("");

                if (memberId.isBlank()) {
                    logger.warn("Failed to update party members!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Member field is blank")
                    );
                    partyHandler.error(event, "Missing Member Information!", "Please provide a member ID or name before continuing.");
                    return;
                }

                VoiceChannel voice = channel.asVoiceChannel();

                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to remove member from party channel because the party does not exist!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );
                        partyHandler.error(event, "Party Not Found!", "Could not remove member because the party channel does not exist in the database.");
                        return;
                    }

                    Member target = findMember(guild,memberId);
                    if (target == null) {
                        logger.warn("Failed to update party members!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason("Target member not found"),
                                new LogData("MemberID", memberId)
                        );
                        partyHandler.error(event, "Member Not Found", "Could not find the specified member. They might have left the server or the ID or name is invalid.");
                        return;
                    }

                    boolean removed = party.getMembers().removeIf(m -> m.getId() == target.getIdLong());

                    if (!removed) {
                        partyHandler.warn(event, "Member Not in Party", "That member is not part of this party.");
                        logger.warn("Failed to update party members!",
                                new LogMember(member),
                                new LogMember("Target", target),
                                new LogData("MemberID", memberId),
                                new LogChannel(channel),
                                new LogReason("Target member is not in party")
                        );
                        return;
                    }

                    partyHandler.cleanPermissions(voice, party.getMembers(),()-> {
                        mongo.save(party);
                        partyHandler.updatePanel(guild, voice, party);

                        partyHandler.successHidden(event, "Channel Members Updated!", "The channel members has been successfully updated.");
                        logger.info("Party channel members has been updated!",
                                new LogMember(member),
                                new LogMember("Target", target),
                                new LogData("MemberID", memberId),
                                new LogChannel(channel)
                        );
                    });

                });
            }
            case "party_channel_member_kick"-> {
                String memberId = Optional.ofNullable(interaction.getValue("member_id")).map(ModalMapping::getAsString).orElse("");

                if (memberId.isBlank()) {
                    logger.warn("Failed to update party members!",
                            new LogMember(member),
                            new LogChannel(channel),
                            new LogReason("Member field is blank")
                    );
                    partyHandler.error(event, "Missing Member Information!", "Please provide a member ID or name before continuing.");
                    return;
                }

                VoiceChannel voice = channel.asVoiceChannel();

                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to kick member from party channel because the party does not exist!",
                                new LogMember(member),
                                new LogChannel(channel)
                        );
                        partyHandler.error(event, "Party Not Found!", "Could not kick member because the party channel does not exist in the database.");
                        return;
                    }
                    Member target = findMember(guild,memberId);
                    if (target == null) {
                        logger.warn("Failed to update party members!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogReason("Target member not found"),
                                new LogData("MemberID", memberId)
                        );
                        partyHandler.error(event, "Member Not Found", "Could not find the specified member. They might have left the server or the ID or name is invalid.");
                        return;
                    }

                    // Disconnect user from voice channel if they are connected
                    if (voice.getMembers().stream().anyMatch(m -> m.getIdLong() == target.getIdLong())) {
                        guild.kickVoiceMember(target).queue(
                            success -> {
                                logger.info("Kicked member from voice channel!",
                                        new LogMember(member),
                                        new LogMember("Target", target),
                                        new LogData("MemberID", memberId),
                                        new LogChannel(channel)
                                );
                                partyHandler.successHidden(event, "Member Kicked!", "The member has been successfully kicked from the voice channel.");
                            },
                            error -> {
                                logger.error("Failed to kick member from voice channel!", error,
                                        new LogMember(member),
                                        new LogMember("Target", target),
                                        new LogData("MemberID", memberId),
                                        new LogChannel(channel)
                                );
                                partyHandler.error(event, "Failed to Kick Member", "Failed to kick the member from the voice channel.");
                            }
                        );
                    } else {
                        logger.warn("Failed to kick member from voice channel!",
                            new LogMember(member),
                            new LogMember("Target", target),
                            new LogData("MemberID", memberId),
                            new LogChannel(channel),
                            new LogReason("Member is not in the voice channel")
                        );
                        partyHandler.error(event, "Member Not in Voice Channel", "The specified member is not currently in the voice channel.");
                    }

                });
            }
        }
    }

    public static boolean isSafeName(String input) {
        if (input == null) return false;

        // Regex includes all allowed characters, properly escaped
        return input.matches("^[a-zA-Z0-9\\-_.+,*!#%&/=?\\\\(){}\\[\\]@£$€^~<>|]*$");
    }
}
