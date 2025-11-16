package fi.natroutter.foxbot.feature.parties.actions.modal;

import fi.natroutter.foxbot.feature.parties.actions.ModalActionTarget;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class PartyRemoveAction extends ModalActionTarget {

    public PartyRemoveAction(ModalInteractionEvent event) {
        super(event);
        if (!validTarget) return;

        mongo.getParties().findByChannelID(voice.getIdLong(), party -> {
            if (party == null) {
                logger.error("Failed to remove member from party channel because the party does not exist!",
                        new LogMember(member),
                        new LogChannel(channel)
                );
                partyHandler.error(event, "Party Not Found!", "Could not remove member because the party channel does not exist in the database.");
                return;
            }

            boolean removed = party.getMembers().removeIf(m -> m.getId() == target.getIdLong());

            if (!removed) {
                partyHandler.warn(event, "Member Not in Party", "That member is not part of this party.");
                logger.warn("Failed to update party members!",
                        new LogMember(member),
                        new LogMember("Target", target),
                        new LogData("TargetID", targetID),
                        new LogChannel(channel),
                        new LogReason("Target member is not in party")
                );
                return;
            }

            partyHandler.cleanPermissions(voice, party.getMembers());
            mongo.save(party);
            partyHandler.updatePanel(guild, voice, party);

            partyHandler.successHidden(event, "Channel Members Updated!", "The channel members has been successfully updated.");
            logger.info("Party channel members has been updated!",
                    new LogMember(member),
                    new LogMember("Target", target),
                    new LogData("TargetID", targetID),
                    new LogChannel(channel)
            );

        });

    }

}
