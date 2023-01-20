package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.Database.MongoHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.NATLogger;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class Permission extends BaseCommand {

    private NATLogger logger = FoxBot.getLogger();
    private MongoHandler mongo = FoxBot.getMongo();

    public Permission() {
        super("permission");
        this.setDescription("manage user/group permissions for FoxBot");
        this.setPermission(Nodes.PERMISSION);

        List<Command.Choice> nodes = new ArrayList<>();
        for (Nodes node : Nodes.values()) {
            nodes.add(new Command.Choice(node.getNode(),node.getNode()));
        }
        nodes.add(new Command.Choice("foxbot.*", "foxbot.*"));

        this.addArguments(
                new OptionData(OptionType.STRING, "action", "Select what you want to do?").setRequired(true)
                        .addChoice("grant","grant")
                        .addChoice("revoke","revoke")
                        .addChoice("show","show"),
                new OptionData(OptionType.ROLE, "role", "Select role!").setRequired(true),
                new OptionData(OptionType.STRING, "permission", "Permission node!").setRequired(false)
                        .addChoices(nodes)

        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Permission Handling!");

        String action = args.get(0).getAsString();
        Role role = args.get(1).getAsRole();

        if (args.size() == 2) {
            if (action.equalsIgnoreCase("show")) {
                mongo.getGroupByID(role.getId(), (data)-> {
                    eb.setTitle("Permission Info");
                    if (data.getPermissions().size() > 0) {
                        String perms = String.join("\n", data.getPermissions());
                        eb.setDescription("**Group:**  _"+role.getAsMention()+"_\n**GroupID:**  _"+role.getId()+"_\n**Permissions:**\n```\n"+perms+"\n```");
                    } else {
                        eb.setDescription("**Group:**  _"+role.getAsMention()+"_\n**GroupID:**  _"+role.getId()+"_\n**Permissions:** _No permissions!_");
                    }

                });

            } else {
                eb.setTitle("Error!");
                eb.setDescription("Invalid action selected.");
            }

        } else if (args.size() == 3) {
            String node = args.get(2).getAsString();
            if (node.length() <= 0) {
                eb.setTitle("Error!");
                eb.setDescription("Invalid permission node.");
                return eb;
            }

            if (action.equalsIgnoreCase("revoke")) {

                eb.setTitle("Permission revoked!");
                eb.setDescription("Revoked permission `" + node + "` from group " + role.getAsMention() + "");

                mongo.getGroupByID(role.getId(), (data) -> {
                    if (data.getPermissions().contains(node)) {
                        data.getPermissions().remove(node);
                        mongo.saveGroup(data);
                        logger.info("Revoked permission \"" + node + "\" from group \"" + role.getName() + "("+role.getId()+")\"");
                    } else {
                        eb.setTitle("Error!");
                        eb.setDescription("That group doesn't have that permission!");
                    }
                });

            } else if (action.equalsIgnoreCase("grant")) {

                eb.setTitle("Permission granted!");
                eb.setDescription("Granted permission `"+node+"` to group "+role.getAsMention()+"");
                mongo.getGroupByID(role.getId(), (data)->{
                    if (!data.getPermissions().contains(node)) {
                        data.getPermissions().add(node);
                        mongo.saveGroup(data);
                        logger.info("Granted permission \"" + node + "\" to group \"" + role.getName() + "("+role.getId()+")\"");
                    } else {
                        eb.setTitle("Error!");
                        eb.setDescription("That group already has that permission!");
                    }
                });

            }
        } else {
            eb.setTitle("Error!");
            eb.setDescription("Invalid arguments.");
        }
        return eb;
    }
}
