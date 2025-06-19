package fi.natroutter.foxbot.feature.parties.modals;

import fi.natroutter.foxbot.feature.parties.data.SlowMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class PartyEditModal implements SimpleModal {

    private int userLimit;
    private int bitRate;
    private Region region;
    private int slowMode;

    @Override
    public Modal build() {
        TextInput userLimitField = TextInput.create("user_limit", "User limit", TextInputStyle.SHORT)
                .setPlaceholder("0-99")
                .setValue(String.valueOf(userLimit))
                .setRequiredRange(1, 2)
                .build();
        TextInput bitRateField = TextInput.create("bitrate", "Bitrate (kbps)", TextInputStyle.SHORT)
                .setPlaceholder("8-96")
                .setValue(String.valueOf(bitRate))
                .setRequiredRange(1, 3)
                .build();
        TextInput regionField = TextInput.create("region", "Region", TextInputStyle.SHORT)
                .setPlaceholder("automatic, brazil, hongkong, india, japan, rotterdam, singapore, south_africa, sydney, us-central...")
                .setValue(region.getKey())
                .setRequiredRange(4, 16)
                .build();
        TextInput slowModeField = TextInput.create("slowmode", "Slowmode", TextInputStyle.SHORT)
                .setPlaceholder(Arrays.stream(SlowMode.values()).map(SlowMode::getArg).collect(Collectors.joining(", ")))
                .setValue(SlowMode.fromValue(slowMode).getArg())
                .setRequiredRange(1, 3)
                .build();

        return Modal.create("party_channel_edit_modal", "Edit Channel")
                .addComponents(
                        ActionRow.of(userLimitField),
                        ActionRow.of(bitRateField),
                        ActionRow.of(regionField),
                        ActionRow.of(slowModeField)
                )
                .build();
    }
}
