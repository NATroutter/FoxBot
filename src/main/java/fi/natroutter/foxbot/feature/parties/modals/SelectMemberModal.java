package fi.natroutter.foxbot.feature.parties.modals;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class SelectMemberModal implements SimpleModal {

    private String id;
    private String title;

    public SelectMemberModal(String id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public Modal build() {
        TextInput targetMember = TextInput.create("member_id", "Member (Id, Username)", TextInputStyle.SHORT)
                .setPlaceholder("162669508866211841")
                .setRequiredRange(1, 32)
                .build();

        return Modal.create(id, title)
                .addComponents(
                        ActionRow.of(targetMember)
                )
                .build();
    }
}
