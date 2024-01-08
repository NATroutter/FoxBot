package fi.natroutter.foxbot.handlers.permissions;

import lombok.Getter;

public enum Node {

    PRUNE("foxbot.prune"),
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
    WAKEUP("foxbot.wakeup"),
    SOCIAL_ADMIN("foxbot.socialcredit.admin"),
    SOCIAL_TOP("foxbot.socialcredit.top"),
    SOCIAL("foxbot.socialcredit"),
    BYPASS_SPAM("foxbot.bypass_spam"),
    INVISTES_SHOW_OTHERS("foxbot.invites.show.others"),
    BYPASS_TRADE_ROLE_REMOVE("foxbot.bypass_trade_role_remove"),
    CATIFY("foxbot.catify"),


    ;
    @Getter
    private String node;
    Node(String node) {
        this.node = node;
    }

}
