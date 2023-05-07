package fi.natroutter.foxbot.objects;

import lombok.Getter;

@Getter
public class BaseReply {

    private boolean isHidden = true;
    private int deleteDelay = 0;
    private Object object;

    public BaseReply setDeleteDelay(int delay) {
        deleteDelay = delay;
        return this;
    }

    public BaseReply setHidden(boolean hidden) {
        isHidden = hidden;
        return this;
    }

    public BaseReply(Object object) {
        this.object = object;
    }

}
