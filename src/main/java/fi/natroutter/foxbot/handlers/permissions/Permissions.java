package fi.natroutter.foxbot.handlers.permissions;

import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.Database.GroupEntry;
import fi.natroutter.foxbot.Database.MongoHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfKeys;
import fi.natroutter.foxbot.configs.ConfigProvider;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.function.Consumer;

public class Permissions {

    private static ConfigProvider config = FoxBot.getConfig();
    private static MongoHandler mongo = FoxBot.getMongo();

    public static void has(Member member, Nodes node, Consumer<Boolean> action) {
        if (config.getLong(ConfKeys.OWNER) == member.getIdLong()) {
            action.accept(true);
            return;
        }
        mongo.getGroups(groups -> {
            for (Role role : member.getRoles()) {
                GroupEntry group = mongo.validateGroup(groups, role.getId(), groups.find(Filters.eq("groupID", role.getId())).first());
                if (group.getPermissions().contains("*")) {
                    action.accept(true);
                    return;
                }
                if (group.getPermissions().contains(node.getNode())) {
                    action.accept(true);
                    return;
                }
            }
        });
        action.accept(false);
    }

}
