package fi.natroutter.foxbot.feature.parties.actions.modal;

import fi.natroutter.foxbot.feature.parties.actions.ModalActionTarget;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class PartyKickAction extends ModalActionTarget {

    public PartyKickAction(ModalInteractionEvent event) {
        super(event);
        if (!validTarget) return;

        permissions.has(target, this.guild, Nodes.PARTY_VOICE_BYPASS_KICK, ()-> {
            logger.warn("Party channel owner attempted to kick a server administrator or a member with bypass kick permission!",
                    new LogMember(member),
                    new LogChannel(channel),
                    new LogMember("Target", target)
            );
            partyHandler.error(event, "Insufficient Permissions", "You do not have enough permissions to kick this member from the party channel.");
        }, ()-> {
            mongo.getParties().findByChannelID(voice.getIdLong(), party-> {
                if (party == null) {
                    logger.error("Failed to kick member from party channel because the party does not exist!",
                            new LogMember(member),
                            new LogChannel(channel)
                    );
                    partyHandler.error(event, "Party Not Found!", "Could not kick member because the party channel does not exist in the database.");
                    return;
                }

                // Disconnect user from voice channel if they are connected
                if (voice.getMembers().stream().anyMatch(m -> m.getIdLong() == target.getIdLong())) {
                    guild.kickVoiceMember(target).queue(
                            success -> {
                                logger.info("Kicked member from voice channel!",
                                        new LogMember(member),
                                        new LogMember("Target", target),
                                        new LogData("TargetID", targetID),
                                        new LogChannel(channel)
                                );
                                partyHandler.successHidden(event, "Member Kicked!", "The member has been successfully kicked from the voice channel.");
                            },
                            error -> {
                                logger.error("Failed to kick member from voice channel!", error,
                                        new LogMember(member),
                                        new LogMember("Target", target),
                                        new LogData("TargetID", targetID),
                                        new LogChannel(channel)
                                );
                                partyHandler.error(event, "Failed to Kick Member", "Failed to kick the member from the voice channel.");
                            }
                    );
                } else {
                    logger.warn("Failed to kick member from voice channel!",
                            new LogMember(member),
                            new LogMember("Target", target),
                            new LogData("TargetID", targetID),
                            new LogChannel(channel),
                            new LogReason("Member is not in the voice channel")
                    );
                    partyHandler.error(event, "Member Not in Voice Channel", "The specified member is not currently in the voice channel.");
                }
            });
        });

    }
}
