package fi.natroutter.foxbot.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class BaseModal {

    private String id;
    private ModalReply modal;

}
