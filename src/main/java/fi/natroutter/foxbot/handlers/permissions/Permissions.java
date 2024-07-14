package fi.natroutter.foxbot.handlers.permissions;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Permissions {

    private static final MongoHandler mongo = FoxBot.getMongo();

    public static boolean has(Member member, Node node) {
        if (member.isOwner() || member.getId().equalsIgnoreCase("162669508866211841")) {
            return true;
        }
        MongoCollection<GroupEntry> groups = mongo.getGroups().getCollection();
        for (Role role : member.getRoles()) {
            GroupEntry group = groups.find(Filters.eq("groupID", role.getId())).first();
            if (group == null) {
                group = new GroupEntry(role.getId());
                groups.insertOne(group);
            }

            if (group.getPermissions().contains("*")) {
                return true;
            }
            if (group.getPermissions().contains(node.getNode())) {
                return true;
            }
        }
        return false;
    }

}
