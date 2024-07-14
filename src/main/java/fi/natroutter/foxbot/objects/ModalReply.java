package fi.natroutter.foxbot.objects;

import lombok.Getter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;

import java.util.List;

public class ModalReply {


    @Getter
    private String modalName;

    @Getter
    private List<ItemComponent> components;

    public ModalReply(String modalName, List<ItemComponent> components){
        this.modalName = modalName;
        this.components = components;
    }

}
