package fi.natroutter.foxbot.handlers.permissions;

import lombok.Getter;

public enum Nodes {

    CLEAN("foxbot.clean"),
    COINFLIP("foxbot.coinflip"),
    DICE("foxbot.dice"),
    PERMISSION("foxbot.permission"),
    STATICS("foxbot.statics"),
    YIFF("foxbot.yiff"),
    UPDATE("foxbot.update");


    @Getter
    private String node;
    Nodes(String node) {
        this.node = node;
    }

}
