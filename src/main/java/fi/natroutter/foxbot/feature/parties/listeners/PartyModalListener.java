package fi.natroutter.foxbot.feature.parties.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.feature.parties.SlowMode;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PartyModalListener extends ListenerAdapter {

    private FoxLogger logger = FoxBot.getLogger();
    private PartyHandler partyHandler = FoxBot.getPartyHandler();
    private MongoHandler mongo = FoxBot.getMongo();

//    TextInput userLimitField = TextInput.create("user_limit", "User limit", TextInputStyle.SHORT)
//            .setPlaceholder(String.valueOf(userLimit))
//            .setRequiredRange(1, 2)
//            .build();
//    TextInput bitRateField = TextInput.create("bitrate", "Bitrate", TextInputStyle.SHORT)
//            .setPlaceholder(String.valueOf(bitRate))
//            .setRequiredRange(1, 3)
//            .build();
//    TextInput regionField = TextInput.create("region", "Region", TextInputStyle.SHORT)
//            .setPlaceholder(region.getKey())
//            .setRequiredRange(4, 16)
//            .build();
//    TextInput slowModeField = TextInput.create("slowmode", "Slowmode", TextInputStyle.SHORT)
//            .setPlaceholder(String.valueOf(slowMode))
//            .setRequiredRange(1, 3)
//            .build();
//
//        return Modal.create("party_channel_edit_modal", "Edit Channel")

    public void missingOptionWarning(IReplyCallback event, String logName,String logChannel, String reason) {
        partyHandler.warn(event, "Failed to edit channel!", reason);
        logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: "+ reason);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        MessageChannelUnion channel = event.getChannel();
        Guild guild = event.getGuild();
        Member member = event.getMember();

        String logName = FoxFrame.getLogName(event.getMember());
        String logChannel = FoxFrame.getLogName(channel);

        ModalInteraction interaction = event.getInteraction();

        switch (id.toLowerCase()) {
            case "party_channel_edit_modal" -> {
                ModalMapping userLimitOpt = interaction.getValue("user_limit");
                ModalMapping bitrateOpt = interaction.getValue("bitrate");
                ModalMapping regionOpt = interaction.getValue("region");
                ModalMapping slowmodeOpt = interaction.getValue("slowmode");

                if (userLimitOpt == null) {
                    missingOptionWarning(event,logName,logChannel, "User limit is not defined");
                    return;
                }
                if (bitrateOpt == null) {
                    missingOptionWarning(event,logName,logChannel, "Bitrate is not defined.");
                    return;
                }
                if (regionOpt == null) {
                    missingOptionWarning(event,logName,logChannel, "region is not defined.");
                    return;
                }
                if (slowmodeOpt == null) {
                    missingOptionWarning(event,logName,logChannel, "Slowmode is not defined.");
                    return;
                }

                //Validations for userLimit
                int userLimit;
                try {
                    userLimit = Integer.parseInt(userLimitOpt.getAsString());
                } catch (NumberFormatException e) {
                    partyHandler.error(event, "User Limit Must Be a Number!", "Please enter a valid number");
                    logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: user limit is not a number (" + userLimitOpt.getAsString() + ")");
                    return;
                }

                if (!FoxLib.isBetween(userLimit, 0,99)) {
                    partyHandler.error(event, "Invalid User Limit Range!", "The user limit must be a number between 0 and 99 inclusive.");
                    logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: user limit is not between 0 and 99");
                    return;
                }

                //Validations for bitRate
                int bitRate;
                try {
                    bitRate = Integer.parseInt(bitrateOpt.getAsString());
                } catch (NumberFormatException e) {
                    partyHandler.error(event, "Bitrate Must Be a Number!", "Please enter a valid number.");
                    logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: bitrate is not a number (" + bitrateOpt.getAsString() + ")");
                    return;
                }

                if (!FoxLib.isBetween(bitRate, 8,96)) {
                    partyHandler.error(event, "Invalid Bitrate Range!", "The bitrate must be a number between 8 and 96 inclusive.");
                    logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: bitrate is not between 8 and 96");
                    return;
                }

                //Validations for region
                Region region = switch (regionOpt.getAsString().toLowerCase()) {
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

                if (region == null) {
                    partyHandler.error(event, "Unsupported Region!", "The region you selected is not allowed. Please select a valid region.\n\nRegions:\n``automatic, brazil, hongkong, india, japan, rotterdam, singapore, south_africa, sydney, us-central, us-east, us-south, us-west``");
                    logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: Invalid region (" + regionOpt.getAsString() + ")");
                    return;
                }

                //Validations for region
                SlowMode slowMode = SlowMode.fromArg(slowmodeOpt.getAsString());
                if (slowMode == null) {
                    String validModes = Arrays.stream(SlowMode.values()).map(SlowMode::getArg).collect(Collectors.joining(", "));
                    logger.warn("Failed to edit party channel " + logChannel + " for user " + logName + " Reason: Invalid slowmode (" + slowmodeOpt.getAsString() + ")");
                    partyHandler.error(event, "Unsupported Slowmode!", "The slowmode you selected is not allowed. Please select a valid slowmode.\n\nSlow modes:\n``"+ validModes  +"``");
                    return;
                }

                VoiceChannel voice = channel.asVoiceChannel();

                //Save selected values to database and update panel etc
                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to edit on party channel because it does not exist " + logChannel);
                        partyHandler.error(event, "Failed to edit!", "Failed to retrieve party channel from database because it does not exists.");
                        return;
                    }

                    int oldUserLimit = voice.getUserLimit();
                    int oldBitRate = voice.getBitrate() / 1000;
                    Region oldRegion = voice.getRegion();
                    int oldSlowMode = voice.getSlowmode();

                    party.setUserLimit(userLimit);
                    party.setBitRate(bitRate);
                    party.setRegion(region.getKey());
                    party.setSlowMode(slowMode.getValue());
                    mongo.save(party);

                    voice.getManager()
                            .setUserLimit(userLimit)
                            .setBitrate(bitRate * 1000)
                            .setRegion(region)
                            .setSlowmode(slowMode.getValue())
                            .queue(s-> {
                                partyHandler.updatePanel(guild, voice);
                            });

                    partyHandler.success(event, "Channel Settings Updated!", "Settings has been successfully updated.");
                    String oldSlow = SlowMode.fromValue(oldSlowMode) != null ? SlowMode.fromValue(oldSlowMode).getArg() : String.valueOf(oldSlowMode);
                    logger.info("Party channel settings has been updated [(UserLimit: "+oldUserLimit+"->"+userLimit+") (Bitrate: "+oldBitRate+"->"+bitRate+") (Region: "+oldRegion.getKey()+"->"+region.getKey()+") (Slowmode: "+oldSlow+"->"+slowMode.getArg()+")] by member " + FoxFrame.getLogName(member));
                });

            }
            case "party_channel_rename_modal" -> {

                ModalMapping newNameOption = interaction.getValue("new_name");

                if (newNameOption == null) {
                    partyHandler.warn(event, "No Name Entered!", "No new name was provided. The current name remains unchanged.");
                    logger.warn("Failed to rename party channel " + logChannel + " for user " + logName + " Reason: No name provided");
                    return;
                }

                String newName = newNameOption.getAsString();
                if (newName.length() > 25) {
                    partyHandler.error(event, "Too Long name!", "Name can not be longer than 25 characters.");
                    logger.error("Failed to rename party channel " + logChannel + " for user " + logName + " Reason: name is longer than 25 characters");
                    return;
                }

                if (!isSafeName(newName)) {
                    partyHandler.error(event, "Name contains illegal characters!", "The name contains one or more illegal characters.\nPlease use only letters, numbers, and the following symbols:\n``- _ . , + * ! # % & / = ? \\ ( ) { } [ ] @ £ $ € ^ ~ < > |``");
                    logger.error("Failed to rename party channel " + logChannel + " for user " + logName + " Reason: name contains illegal characters");
                    return;
                }

                VoiceChannel voice = channel.asVoiceChannel();

                mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                    if (party == null) {
                        logger.error("Failed to update name on party channel because it does not exist " + logChannel);
                        partyHandler.error(event, "Failed to update name!", "Failed to retrieve party channel from database because it does not exists.");
                        return;
                    }

                    String oldName = voice.getName();

                    party.setName(newName);
                    mongo.save(party);

                    voice.getManager().setName(newName).queue(s->{
                        partyHandler.updatePanel(guild, voice);
                    });

                    partyHandler.success(event, "Channel Name Updated!", "The channel name has been successfully updated.");
                    logger.info("Party channel has been renamed from "+oldName+" to "+newName+" by member " + FoxFrame.getLogName(member));
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
