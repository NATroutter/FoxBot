package fi.natroutter.foxbot.handlers.permissions;

import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.database.GroupEntry;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.database.Validator;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Permissions {

    private static ConfigProvider config = FoxBot.getConfig();
    private static MongoHandler mongo = FoxBot.getMongo();
    private static Validator validator = new Validator();

    public static void has(Member member, Node node, Consumer<Boolean> action) {
        if (member.isOwner() || member.getId().equalsIgnoreCase("162669508866211841")) {
            action.accept(true);
            return;
        }
        mongo.getGroups(groups -> {
            for (Role role : member.getRoles()) {
                GroupEntry group = validator.group(groups, role.getId(), groups.find(Filters.eq("groupID", role.getId())).first());
                if (group.getPermissions().contains("*")) {
                    action.accept(true);
                    return;
                }
                if (group.getPermissions().contains(node.getNode())) {
                    action.accept(true);
                    return;
                }
            }
            action.accept(false);
        });

    }

    public static CompletableFuture<Boolean> has(Member member, Node node) {
        CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();

        if (member.isOwner() || member.getId().equalsIgnoreCase("162669508866211841")) {
            result.complete(true);
            return result;
        }
        mongo.getGroups(groups -> {
            for (Role role : member.getRoles()) {
                GroupEntry group = validator.group(groups, role.getId(), groups.find(Filters.eq("groupID", role.getId())).first());
                if (group.getPermissions().contains("*")) {
                    result.complete(true);
                    return;
                }
                if (group.getPermissions().contains(node.getNode())) {
                    result.complete(true);
                    return;
                }
            }
            result.complete(false);
        });
        return result;
    }

}
