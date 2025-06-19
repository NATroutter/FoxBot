package fi.natroutter.foxbot.feature.parties.modals;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

@Getter
@AllArgsConstructor
public class PartyRenameModal implements SimpleModal {

    private Member member;
    private String currentName;

    @Override
    public Modal build() {
        TextInput newNameField = TextInput.create("new_name", "New name", TextInputStyle.SHORT)
                .setPlaceholder(member.getUser().getGlobalName()+"'s Voice")
                .setValue(currentName)
                .setRequiredRange(1, 25)
                .build();

        return Modal.create("party_channel_rename_modal", "Rename Channel")
                .addComponents(ActionRow.of(newNameField))
                .build();
    }
}
