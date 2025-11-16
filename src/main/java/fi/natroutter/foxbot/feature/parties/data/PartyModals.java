package fi.natroutter.foxbot.feature.parties.data;

import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
//import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.modals.Modal;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PartyModals {

    public static Modal kickMember() {
        TextInput targetMember = TextInput.create("target_id", TextInputStyle.SHORT)
                .setPlaceholder("162669508866211841")
                .setRequiredRange(1, 32)
                .build();

        return Modal.create("party_channel_member_kick", "Remove Member")
                .addComponents(
                        Label.of("Member (Id, Username)", targetMember))

                .build();
    }

    public static Modal removeMember() {
        TextInput targetMember = TextInput.create("target_id", TextInputStyle.SHORT)
                .setPlaceholder("162669508866211841")
                .setRequiredRange(1, 32)
                .build();

        return Modal.create("party_channel_member_remove", "Kick Member")
                .addComponents(
                        Label.of("Member (Id, Username)", targetMember)
                )
                .build();
    }

    public static Modal addMember() {
        TextInput targetMember = TextInput.create("target_id", TextInputStyle.SHORT)
                .setPlaceholder("162669508866211841")
                .setRequiredRange(1, 32)
                .build();

        TextInput isAdmin = TextInput.create("member_admin", TextInputStyle.SHORT)
                .setPlaceholder("YES or NO")
                .setValue("NO")
                .setRequiredRange(1, 3)
                .build();

        return Modal.create("party_channel_member_add", "Add Member")
                .addComponents(
                        Label.of("Member (Id, Username)", targetMember),
                        Label.of("Channel admin?", isAdmin)
                )
                .build();
    }

    public static Modal renameChannel(Member member, String currentName) {
        TextInput newNameField = TextInput.create("new_name", TextInputStyle.SHORT)
                .setPlaceholder(member.getUser().getGlobalName() + "'s Voice")
                .setValue(currentName)
                .setRequiredRange(1, 25)
                .build();

        return Modal.create("party_channel_rename_modal", "Rename Channel")
                .addComponents(
                        Label.of("New name", newNameField)
                )
                .build();
    }

    public static Modal editChannel(int userLimit, int bitRate, RealRegion region, int slowMode) {
        TextInput userLimitField = TextInput.create("user_limit", TextInputStyle.SHORT)
                .setPlaceholder("0-99")
                .setValue(String.valueOf(userLimit))
                .setRequiredRange(1, 2)
                .build();
        TextInput bitRateField = TextInput.create("bitrate", TextInputStyle.SHORT)
                .setPlaceholder("8-96")
                .setValue(String.valueOf(bitRate))
                .setRequiredRange(1, 3)
                .build();
        TextInput regionField = TextInput.create("region", TextInputStyle.SHORT)
                .setPlaceholder("automatic, brazil, hongkong, india, japan, rotterdam, singapore, south_africa, sydney, us-central...")
                .setValue(region.getKey())
                .setRequiredRange(4, 16)
                .build();
        TextInput slowModeField = TextInput.create("slowmode", TextInputStyle.SHORT)
                .setPlaceholder(Arrays.stream(SlowMode.values()).map(SlowMode::getArg).collect(Collectors.joining(", ")))
                .setValue(SlowMode.fromValue(slowMode).getArg())
                .setRequiredRange(1, 3)
                .build();

        return Modal.create("party_channel_edit_modal", "Edit Channel")
                .addComponents(
                        Label.of("User limit", userLimitField),
                        Label.of("Bitrate (kbps)", bitRateField),
                        Label.of("Region", regionField),
                        Label.of("Slowmode", slowModeField)
                )
                .build();
    }

}
