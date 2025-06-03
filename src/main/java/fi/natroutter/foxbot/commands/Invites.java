package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.handlers.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Invites extends BaseCommand {

    private MongoHandler mongo = FoxBot.getMongo();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public Invites() {
        super("invites");
        this.setDescription("Shows how many users you have invited!");
        this.setHidden(true);
        this.addArguments(
                new OptionData(OptionType.STRING, "action", "What you want to do?").setRequired(false)
                        .addChoice("show", "show"),
                new OptionData(OptionType.USER, "user", "Target user!").setRequired(false)
        );
    }

    @Override @SneakyThrows
    public Object onCommand(JDA jda, Member member, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        OptionMapping actOPT = getOption(args, "action");
        if (actOPT == null) {
            return showInvites(member.getUser());
        }
        String action = actOPT.getAsString();
        switch (action.toLowerCase()) {
            case "show" -> {
                OptionMapping targetOPT = getOption(args, "user");
                if (targetOPT == null) {
                    return showInvites(member.getUser());
                }

                if (!perms.has(member, guild, Nodes.INVISTES_SHOW_OTHERS).get(10, TimeUnit.SECONDS)) {
                    return error("You don't have permission to do that!");
                }

                User target = targetOPT.getAsUser();
                return showInvites(target);
            }
        }
        return error("Unknown action!");
    }

    private EmbedBuilder showInvites(User target) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Invites");
        eb.setThumbnail(target.getAvatarUrl());
        mongo.getUsers().getInviteCont(target.getId(), count -> {
            eb.setDescription("Total: " + count);
        });
        return eb;
    }
}
