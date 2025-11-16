package fi.natroutter.foxbot.feature.parties.actions.modal;

import fi.natroutter.foxbot.feature.parties.actions.ModalAction;
import fi.natroutter.foxbot.feature.parties.data.PartyChange;
import fi.natroutter.foxbot.feature.parties.data.RealRegion;
import fi.natroutter.foxbot.feature.parties.data.SlowMode;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogError;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class PartyEditAction extends ModalAction {

    public PartyEditAction(ModalInteractionEvent event) {
        super(event);

        Integer userLimit = Optional.ofNullable(modalInteraction.getValue("user_limit")).map(ModalMapping::getAsString).map(this::tryParseInt).orElse(null);
        Integer bitRate = Optional.ofNullable(modalInteraction.getValue("bitrate")).map(ModalMapping::getAsString).map(this::tryParseInt).orElse(null);
        RealRegion region = Optional.ofNullable(modalInteraction.getValue("region")).map(ModalMapping::getAsString).map(RealRegion::fromKey).orElse(null);
        String slowModeOPT = Optional.ofNullable(modalInteraction.getValue("slowmode")).map(ModalMapping::getAsString).orElse(null);

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

        if (!FoxLib.isBetween(userLimit, 0, 99)) {
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

        if (!FoxLib.isBetween(bitRate, 8, 96)) {
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
            partyHandler.error(event, "Unsupported Region!", "The region you selected is not allowed. Please select a valid region.\n\nRegions:\n``"+RealRegion.regionList()+"``");
            logger.warn("Failed to edit party channel!",
                    new LogMember(member),
                    new LogChannel(channel),
                    new LogReason("Invalid region")
            );
            return;
        }

        //Validations for slowmode
        if (slowModeOPT == null) {
            String validModes = Arrays.stream(SlowMode.values()).map(SlowMode::getArg).collect(Collectors.joining(", "));
            logger.warn("Failed to edit party channel!",
                    new LogMember(member),
                    new LogChannel(channel),
                    new LogReason("Invalid slowmode")
            );
            partyHandler.error(event, "Unsupported Slowmode!", "The slowmode you selected is not allowed. Please select a valid slowmode.\n\nSlow modes:\n``" + validModes + "``");
            return;
        }
        SlowMode sMode = SlowMode.fromArg(slowModeOPT);

        //Save selected values to database and update panel etc
        mongo.getParties().findByChannelID(voice.getIdLong(), party -> {
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
                        new LogData("UserLimit", oldUserLimit + "->" + userLimit),
                        new LogData("Bitrate", oldBitRate + "->" + bitRate),
                        new LogData("Region", oldRegion.getKey() + "->" + region.getKey()),
                        new LogData("SlowMode", SlowMode.fromValue(oldSlowMode).getArg() + "->" + sMode.getArg()),
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
                    .setRegion(region.toDiscord())
                    .setSlowmode(sMode.getValue())
                    .queue(s -> {
                        mongo.save(party);
                        partyHandler.updatePanel(guild, voice, party);

                        partyHandler.success(event, "Channel Settings Updated!", "Settings has been successfully updated.",
                                new PartyChange("UserLimit", oldUserLimit, userLimit),
                                new PartyChange("Bitrate", oldBitRate, bitRate),
                                new PartyChange("Region", oldRegion.getKey(), region.getKey()),
                                new PartyChange("SlowMode", oldSlow, sMode.getArg())
                        );

                        logger.info("Party channel settings has been updated",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogData("UserLimit", oldUserLimit + "->" + userLimit),
                                new LogData("Bitrate", oldBitRate + "->" + bitRate),
                                new LogData("Region", oldRegion.getKey() + "->" + region.getKey()),
                                new LogData("SlowMode", oldSlow + "->" + sMode.getArg())
                        );
                    }, error -> {
                        logger.error("Failed to edit party channel!",
                                new LogMember(member),
                                new LogChannel(channel),
                                new LogData("UserLimit", oldUserLimit + "->" + userLimit),
                                new LogData("Bitrate", oldBitRate + "->" + bitRate),
                                new LogData("Region", oldRegion.getKey() + "->" + region.getKey()),
                                new LogData("SlowMode", oldSlow + "->" + sMode.getArg()),
                                new LogError(error)
                        );
                        partyHandler.error(event, "Failed to Edit Channel!", error.getMessage());
                    });
        });
    }

    private Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
