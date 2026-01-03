package fi.natroutter.foxbot.permissions;

import fi.natroutter.foxframe.permissions.INode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum Nodes implements INode {

    BYPASS_COOLDOWN_COMMAND("foxbot.bypass_cooldown_command", "Bypass Command Cooldowns"),
    BYPASS_COOLDOWN_BUTTON("foxbot.bypass_cooldown_button", "Bypass Button Cooldowns"),
    PRUNE("foxbot.prune", "Use \"/prune\" to cleanup channel messages"),
    COINFLIP("foxbot.coinflip", "Use \"/coinflip\" to try theirs chance"),
    DICE("foxbot.dice", "Use \"/dice\" to throw a dice"),
    PERMISSION("foxbot.permission", "Use permission management system"),
    INFO_SERVER("foxbot.info.server", "Use \"/info\" for server information"),
    INFO_USER("foxbot.info.user", "Use \"/info\" for user information"),
    INFO_ROLE("foxbot.info.role", "Use \"/info\" for role information"),
    YIFF("foxbot.yiff", "Use \"/yiff\" to get receive traumatic experiences"),
    EMBED("foxbot.embed", "Use \"/embed\" to send custom embed messages"),
    BYPASS_WAKEUP("foxbot.bypass_wakeup", "Bypass wakeup attempts by others users"),
    ASK("foxbot.ask", "Use \"/ask\" to get answerer from foxbot"),
    PICK("foxbot.pick", "Use \"/pick\" to make foxbot choose for you"),
    WAKEUP("foxbot.wakeup", "Use \"/wakeup\" to try wakeup users by throwing them around channels"),
    SOCIAL_ADMIN("foxbot.socialcredit.admin", "Social credit admin"),
    SOCIAL_TOP("foxbot.socialcredit.top", "Social credit toplist"),
    SOCIAL("foxbot.socialcredit", "Use social credit system"),
    BYPASS_SPAM("foxbot.bypass_spam", "Bypass spam filters"),
    INVISTES_SHOW_OTHERS("foxbot.invites.show.others", "Use \"/invites\" to inspect other peoples invite info"),
    CATIFY("foxbot.catify", "Use \"/catify\" to convert messages to cat speech"),
    FOX("foxbot.fox", "Use \"/fox\" for random fox pictures"),
    PARTY_VOICE("foxbot.party_voice", "Use party voice system"),
    PARTY_VOICE_RENAME("foxbot.party_voice.rename", "Rename own party channel"),
    PARTY_VOICE_BYPASS_COOLDOWN("foxbot.party_voice.bypass_cooldown", "Bypass interaction cooldown on party channels"),
    PARTY_VOICE_BYPASS_KICK("foxbot.party_voice.bypass_kick", "Bypass kick attempts by party channel owner"),
    GRAMMAR("foxbot.grammar", "Fix grammar and spelling mistakes on inputted text"),
    SHORTER("foxbot.shorten", "Shorten urls"),
    ;

    private String node;
    private String name;

    public static boolean isValidNode(String node) {
        return Arrays.stream(Nodes.values())
                .anyMatch(entry -> entry.getNode().equalsIgnoreCase(node));
    }
}