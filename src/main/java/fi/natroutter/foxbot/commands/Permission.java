package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.GroupEntry;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
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

    private FoxLogger logger = FoxBot.getLogger();
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
                new OptionData(OptionType.STRING, "node", "Permission node!").setRequired(false)
                        .addChoices(nodes)

        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Permission Handling!");

        OptionMapping action = getOption(args, "action");
        if (action == null) {return error("action is not defined!");}


        if (args.size() == 2) {
            if (action.getAsString().equalsIgnoreCase("show")) {
                OptionMapping roleOpt = getOption(args, "role");
                if (roleOpt == null) {return error("Role is not defined!");}
                Role role = roleOpt.getAsRole();

                mongo.getGroups().findByID(role.getId(), (data)-> {
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
            //Get role argument
            OptionMapping roleOpt = getOption(args, "role");
            if (roleOpt == null) {return error("Role is not defined!");}
            Role role = roleOpt.getAsRole();

            //get permission node argument!
            OptionMapping nodeOpt = getOption(args, "node");
            if (nodeOpt == null) {return error("permission node is not defined!");}
            String node = nodeOpt.getAsString();
            if (node.length() <= 0) {
                eb.setTitle("Error!");
                eb.setDescription("Invalid permission node.");
                return eb;
            }

            if (action.getAsString().equalsIgnoreCase("revoke")) {

                eb.setTitle("Permission revoked!");
                eb.setDescription("Revoked permission `" + node + "` from group " + role.getAsMention() + "");

                mongo.getGroups().findByID(role.getId(), (data) -> {
                    if (data.getPermissions().contains(node)) {
                        data.getPermissions().remove(node);
                        mongo.save(data);
                        logger.info("Revoked permission \"" + node + "\" from group \"" + role.getName() + "("+role.getId()+")\"");
                    } else {
                        eb.setTitle("Error!");
                        eb.setDescription("That group doesn't have that permission!");
                    }
                });

            } else if (action.getAsString().equalsIgnoreCase("grant")) {

                eb.setTitle("Permission granted!");
                eb.setDescription("Granted permission `"+node+"` to group "+role.getAsMention()+"");
                mongo.getGroups().findByID(role.getId(), (data)->{
                    if (!data.getPermissions().contains(node)) {
                        data.getPermissions().add(node);
                        mongo.save(data);
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
