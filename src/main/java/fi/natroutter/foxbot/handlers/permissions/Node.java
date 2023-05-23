package fi.natroutter.foxbot.handlers.permissions;

import lombok.Getter;

public enum Node {

    CLEAN("foxbot.clean"),
    COINFLIP("foxbot.coinflip"),
    DICE("foxbot.dice"),
    PERMISSION("foxbot.permission"),
    INFO_SERVER("foxbot.info.server"),
    INFO_USER("foxbot.info.user"),
    INFO_ROLE("foxbot.info.role"),
    YIFF("foxbot.yiff"),
    UPDATE("foxbot.update"),
    BYPASS_COOLDOWN("foxbot.bypass_cooldown"),
    BYPASS("foxbot.bypass"),
    ASK("foxbot.ask"),
    PICK("foxbot.pick"),
    WAKEUP("foxbot.wakeup");


    @Getter
    private String node;
    Node(String node) {
        this.node = node;
    }

}
