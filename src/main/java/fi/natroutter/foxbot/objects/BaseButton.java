package fi.natroutter.foxbot.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

@Getter @Setter @AllArgsConstructor
public class BaseButton {

    private String id;
    private Button button;

}
