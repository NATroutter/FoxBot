package fi.natroutter.foxbot.feature.parties.actions.modal;

import fi.natroutter.foxbot.database.models.PartyEntry;
import fi.natroutter.foxbot.feature.parties.actions.ModalAction;
import fi.natroutter.foxbot.feature.parties.actions.ModalActionTarget;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogError;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Optional;

public class PartyAddAction extends ModalActionTarget {

    public PartyAddAction(ModalInteractionEvent event) {
        super(event);
        if (!validTarget) return;

        String memberAdmin = Optional.ofNullable(modalInteraction.getValue("member_admin")).map(ModalMapping::getAsString).orElse("");

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

        mongo.getParties().findByChannelID(voice.getIdLong(), party -> {
            if (party == null) {
                logger.error("Failed to add member from party channel because the party does not exist!",
                        new LogMember(member),
                        new LogChannel(channel)
                );
                partyHandler.error(event, "Party Not Found!", "Could not add member because the party channel does not exist in the database.");
                return;
            }

            if (target.getId().equalsIgnoreCase(party.getOwnerID())) {
                logger.warn("Party member update blocked.",
                        new LogMember(member),
                        new LogMember("Target", target),
                        new LogChannel(channel),
                        new LogData("TargetID", targetID),
                        new LogData("AdminStatus", memberAdmin),
                        new LogReason("Attempted to add the party owner as a regular member")
                );
                partyHandler.error(event, "Invalid Member", "The party owner is already part of the party and cannot be added as a regular member.");
                return;
            }


            party.getMembers().add(new PartyEntry.PartyMember(target.getIdLong(), target.getUser().getName(), admin));

            partyHandler.setupPermissions(voice, target, admin, () -> {
                mongo.save(party);
                partyHandler.updatePanel(guild, voice, party);

                partyHandler.successHidden(event, "Channel Members Updated!", "The channel members has been successfully updated.");
                logger.info("Party channel members has been updated!",
                        new LogMember(member),
                        new LogMember("Target", target),
                        new LogChannel(channel),
                        new LogData("TargetID", targetID),
                        new LogData("AdminStatus", memberAdmin),
                        new LogData("AddedMember", new LogMember(target))
                );
            }, (error) -> {
                logger.error("Failed to update party channel members!",
                        new LogMember(member),
                        new LogMember("Target", target),
                        new LogChannel(channel),
                        new LogData("TargetID", targetID),
                        new LogData("AdminStatus", memberAdmin),
                        new LogError(error)
                );
                partyHandler.error(event, "Failed to Update Members!", "Failed to setup permissions for the member.");
            });
        });
    }
}
