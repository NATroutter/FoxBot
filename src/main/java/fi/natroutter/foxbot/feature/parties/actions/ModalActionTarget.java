package fi.natroutter.foxbot.feature.parties.actions;

import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogReason;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Optional;

public abstract class ModalActionTarget extends ModalAction {

    protected boolean validTarget = false;
    protected String targetID;
    protected Member target;

    public ModalActionTarget(ModalInteractionEvent event) {
        super(event);

        this.targetID = Optional.ofNullable(modalInteraction.getValue("target_id")).map(ModalMapping::getAsString).orElse("");

        if (this.targetID.isBlank()) {
            logger.warn("Failed to update party members!",
                    new LogMember(member),
                    new LogChannel(channel),
                    new LogReason("Member field is blank")
            );
            partyHandler.error(event, "Missing Member Information!", "Please provide a member ID or name before continuing.");
            return;
        }

        this.target = findMember(guild,targetID);
        if (this.target == null) {
            logger.warn("Failed to update party members!",
                    new LogMember(member),
                    new LogChannel(channel),
                    new LogReason("Target member not found"),
                    new LogData("TargetID", targetID)
            );
            partyHandler.error(event, "Member Not Found", "Could not find the specified member. They might have left the server or the ID or name is invalid.");
            return;
        }
        validTarget = true;
    }

}
