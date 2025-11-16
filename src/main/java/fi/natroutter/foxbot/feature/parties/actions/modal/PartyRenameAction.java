package fi.natroutter.foxbot.feature.parties.actions.modal;

import fi.natroutter.foxbot.feature.parties.actions.ModalAction;
import fi.natroutter.foxbot.feature.parties.data.PartyChange;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogError;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Optional;

public class PartyRenameAction extends ModalAction {

    public PartyRenameAction(ModalInteractionEvent event) {
        super(event);
        String oldName = voice.getName();
        String newName = Optional.ofNullable(modalInteraction.getValue("new_name")).map(ModalMapping::getAsString).orElse("");

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
                    new LogData("NewName", newName.substring(0, 25) + "..."),
                    new LogReason("Name is longer than 25 characters")
            );
            return;
        }

        if (!partyHandler.isSafeName(newName)) {
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

        mongo.getParties().findByChannelID(voice.getIdLong(), party -> {
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

            voice.getManager().setName(newName).queue(s -> {
                partyHandler.updatePanel(guild, voice, party);
                mongo.save(party);

                partyHandler.success(event, "Channel Name Updated!", "The channel name has been successfully updated.",
                        new PartyChange("Name", oldName, newName)
                );
                logger.info("Party channel has been renamed!",
                        new LogMember(member),
                        new LogChannel(channel),
                        new LogData("OldName", oldName),
                        new LogData("NewName", newName)
                );

            }, error -> {
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
}
