package fi.natroutter.foxbot.handlers.permissions;

import lombok.Getter;

public enum Node {

    CLEAN("foxbot.clean"),
    COINFLIP("foxbot.coinflip"),
    DICE("foxbot.dice"),
    PERMISSION("foxbot.permission"),
    STATICS("foxbot.statics"),
    YIFF("foxbot.yiff"),
    UPDATE("foxbot.update"),
    BYPASS_COOLDOWN("foxbot.bypass_cooldown");


    @Getter
    private String node;
    Node(String node) {
        this.node = node;
    }

}
