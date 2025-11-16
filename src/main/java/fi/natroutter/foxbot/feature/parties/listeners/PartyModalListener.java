package fi.natroutter.foxbot.feature.parties.listeners;

import fi.natroutter.foxbot.feature.parties.actions.modal.*;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class PartyModalListener extends ListenerAdapter {

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String id = event.getModalId();
        if (event.getGuild() == null) return;

        switch (id.toLowerCase()) {
            case "party_channel_edit_modal" -> new PartyEditAction(event);
            case "party_channel_member_remove" -> new PartyRemoveAction(event);
            case "party_channel_member_add" -> new PartyAddAction(event);
            case "party_channel_member_kick" -> new PartyKickAction(event);
            case "party_channel_rename_modal" -> new PartyRenameAction(event);
        }
    }
}
