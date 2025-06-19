package fi.natroutter.foxbot.feature.parties.modals;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

public class AddMemberModal implements SimpleModal {

    @Override
    public Modal build() {
        TextInput targetMember = TextInput.create("member_id", "Member (Id, Username)", TextInputStyle.SHORT)
                .setPlaceholder("162669508866211841")
                .setRequiredRange(1, 32)
                .build();

        TextInput isAdmin = TextInput.create("member_admin", "Channel admin?", TextInputStyle.SHORT)
                .setPlaceholder("YES or NO")
                .setValue("NO")
                .setRequiredRange(1, 3)
                .build();

        return Modal.create("party_channel_member_add", "Add Member")
                .addComponents(
                        ActionRow.of(targetMember),
                        ActionRow.of(isAdmin)
                )
                .build();
    }
}
