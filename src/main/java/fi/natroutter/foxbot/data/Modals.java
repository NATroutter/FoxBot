package fi.natroutter.foxbot.data;

import fi.natroutter.foxframe.components.BaseModal;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.List;

public class Modals {

    public static BaseModal minecraftApplication() {
        TextInput name = TextInput.create("name", "What is your minecraft name?", TextInputStyle.SHORT)
                .setMinLength(3)
                .setMaxLength(16)
                .setRequired(true)
                .build();

        TextInput old = TextInput.create("old", "How old are you?", TextInputStyle.SHORT)
                .setRequired(true)
                .setMaxLength(3)
                .build();

        TextInput howlong = TextInput.create("howlong", "How long have you been playing Minecraft?", TextInputStyle.SHORT)
                .setRequired(true)
                .build();

        TextInput why = TextInput.create("why", "Why you want to be whitelisted?", TextInputStyle.PARAGRAPH)
                .setMinLength(15)
                .setRequired(true)
                .build();

        TextInput what = TextInput.create("what", "What are you planing to do if you get listed?", TextInputStyle.PARAGRAPH)
                .setMinLength(15)
                .setRequired(true)
                .build();

        return new BaseModal("whitelist_application", "Whitelist Application",List.of(name,old,howlong,why,what));
    }

}
