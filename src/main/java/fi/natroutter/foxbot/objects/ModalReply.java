package fi.natroutter.foxbot.objects;

import lombok.Getter;
import net.dv8tion.jda.api.interactions.components.ActionRow;

import java.util.List;

public class ModalReply {


    @Getter
    private String modalName;

    @Getter
    private List<ActionRow> rows;

    public ModalReply(String modalName, List<ActionRow> rows){
        this.modalName = modalName;
        this.rows = rows;
    }

}
