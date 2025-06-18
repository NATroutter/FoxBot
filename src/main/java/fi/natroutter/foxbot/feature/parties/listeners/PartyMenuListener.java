package fi.natroutter.foxbot.feature.parties.listeners;

import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class PartyMenuListener extends ListenerAdapter {

    //    EntitySelectMenu memberMenu = EntitySelectMenu.create("party_member_list", EntitySelectMenu.SelectTarget.USER)
//            .setDefaultValues(oldMembers)
//            .build();

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        super.onEntitySelectInteraction(event);
    }

}
