package fi.natroutter.foxbot.handlers.permissions;

import fi.natroutter.foxframe.permissions.INode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Nodes implements INode {

    PRUNE("foxbot.prune"),
    COINFLIP("foxbot.coinflip"),
    DICE("foxbot.dice"),
    PERMISSION("foxbot.permission"),
    INFO_SERVER("foxbot.info.server"),
    INFO_USER("foxbot.info.user"),
    INFO_ROLE("foxbot.info.role"),
    YIFF("foxbot.yiff"),
    EMBED("foxbot.embed"),
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
    FOX("foxbot.fox"),
    ;

    private String node;
}