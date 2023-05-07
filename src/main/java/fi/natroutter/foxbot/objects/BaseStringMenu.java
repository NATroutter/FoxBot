package fi.natroutter.foxbot.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

@Getter
@Setter
@AllArgsConstructor
public class BaseStringMenu {

    private String id;
    private StringSelectMenu menu;

}
