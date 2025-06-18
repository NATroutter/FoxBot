package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Permission extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private MongoHandler mongo = FoxBot.getMongo();

    public Permission() {
        super("permission");
        this.setDescription("manage user/group permissions for FoxBot");
        this.setPermission(Nodes.PERMISSION);
    }

    @Override
    public List<OptionData> options() {
        List<Command.Choice> nodes = new ArrayList<>();

//        for (Nodes node : Nodes.values()) {
//            nodes.add(new Command.Choice(node.getNode() ,node.getNode()));
//        }
//        nodes.add(new Command.Choice("foxbot.*", "foxbot.*"));

        return List.of(
                new OptionData(OptionType.STRING, "action", "Select what you want to do?").setRequired(true)
                        .addChoice("list","list")
                        .addChoice("grant","grant")
                        .addChoice("revoke","revoke")
                        .addChoice("show","show"),
                new OptionData(OptionType.ROLE, "role", "Select role!").setRequired(false),
                new OptionData(OptionType.STRING, "node", "Permission node!").setRequired(false)
        );
    }

    public String getNodeOption(SlashCommandInteractionEvent event) {
        OptionMapping nodeOPT = event.getOption("node");
        if (nodeOPT == null) {
            errorMessage(event, "Permission node is not defined!");
            return null;
        }
        String node = nodeOPT.getAsString();
        if (node.isEmpty()) {
            errorMessage(event, "Invalid permission node!");
            return null;
        }
        return node;
    }
    public Role getRoleOption(SlashCommandInteractionEvent event) {
        OptionMapping roleOPT = event.getOption("role");
        if (roleOPT == null) {
            errorMessage(event, "Role is not defined!");
            return null;
        }
        return roleOPT.getAsRole();
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Permission Handling!");

        String action = Objects.requireNonNull(event.getOption("action")).getAsString();

        switch (action.toLowerCase()) {
            case "list" -> {
                String list = Arrays.stream(Nodes.values())
                        .map(node-> "• ``"+node.getNode() + "`` - *" + node.getName() + "*")
                        .collect(Collectors.joining("\n"));

                list = list + "\n• ``foxbot.*`` - *Full admin access*";
                eb.setTitle("Permission List");
                eb.setDescription("\n"+list);
            }
            case "show" -> {
                Role role = getRoleOption(event);
                if (role == null) return;

                mongo.getGroups().findByID(role.getId(), (data)-> {
                    eb.setTitle("Permission Info");
                    if (!data.getPermissions().isEmpty()) {
                        String perms = String.join("\n", data.getPermissions());
                        eb.setDescription("**Group:**  *"+role.getAsMention()+"*\n**GroupID:**  *"+role.getId()+"*\n**Permissions:**\n```\n"+perms+"\n```");
                    } else {
                        eb.setDescription("**Group:**  *"+role.getAsMention()+"*\n**GroupID:**  *"+role.getId()+"*\n**Permissions:** *No permissions!*");
                    }
                });
            }
            case "revoke" -> {
                Role role = getRoleOption(event);
                if (role == null) return;

                String node = getNodeOption(event);
                if (node == null) return;

                eb.setTitle("Permission revoked!");
                eb.setDescription("Revoked permission `" + node + "` from group " + role.getAsMention());

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
            }
            case "grant" -> {
                Role role = getRoleOption(event);
                if (role == null) return;

                String node = getNodeOption(event);
                if (node == null) return;

                if (!Nodes.isValidNode(node)) {
                    errorMessage(event, "That node does not exists!", true);
                    return;
                }

                eb.setTitle("Permission granted!");
                eb.setDescription("Granted permission `"+node+"` to group "+role.getAsMention());
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
            default -> {
                eb.setTitle("Error!");
                eb.setDescription("Invalid arguments.");
            }
        }
        reply(event, eb);
    }
}
