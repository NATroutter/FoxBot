package fi.natroutter.foxbot.objects;

import lombok.Getter;

@Getter
public class BaseReply {

    private boolean isHidden = true;
    private Object object;

    public BaseReply setHidden(boolean hidden) {
        isHidden = hidden;
        return this;
    }

    public BaseReply(Object object) {
        this.object = object;
    }

}
