package fi.natroutter.foxbot.permissions;

import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxframe.permissions.INode;
import fi.natroutter.foxframe.permissions.IPermissionHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.concurrent.CompletableFuture;

public class PermissionHandler extends IPermissionHandler {

    private final MongoHandler mongo = FoxBot.getMongo();

    @Override
    public void has(Member member, Guild guild, INode node, Runnable success, Runnable failed) {
        if (member.isOwner() || member.getId().equalsIgnoreCase("162669508866211841")) {
            success.run();
            return;
        }
        mongo.getGroups().getCollection(groups -> {
            for (Role role : member.getRoles()) {
                GroupEntry group = groups.find(Filters.eq("groupID", role.getId())).first();
                if (group == null) {
                    group = new GroupEntry(role.getId());
                    groups.insertOne(group);
                }

                if (group.getPermissions().contains("*")) {
                    success.run();
                    return;
                }
                if (group.getPermissions().contains(node.getNode())) {
                    success.run();
                    return;
                }
            }
            failed.run();
        });
    }

    @Override
    public CompletableFuture<Boolean> has(Member member, Guild guild, INode nodes) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        if (member.isOwner() || member.getId().equalsIgnoreCase("162669508866211841")) {
            result.complete(true);
            return result;
        }
        mongo.getGroups().getCollection(groups -> {
            for (Role role : member.getRoles()) {
                GroupEntry group = groups.find(Filters.eq("groupID", role.getId())).first();
                if (group == null) {
                    group = new GroupEntry(role.getId());
                    groups.insertOne(group);
                }

                if (group.getPermissions().contains("*")) {
                    result.complete(true);
                    return;
                }
                if (group.getPermissions().contains(nodes.getNode())) {
                    result.complete(true);
                    return;
                }
            }
            result.complete(false);
        });
        return result;
    }

}
