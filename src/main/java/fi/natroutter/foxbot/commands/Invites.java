package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Invites extends DiscordCommand {

    private MongoHandler mongo = FoxBot.getMongo();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public Invites() {
        super("invites");
        this.setDescription("Shows how many users you have invited!");
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "action", "What you want to do?").setRequired(false)
                        .addChoice("show", "show"),
                new OptionData(OptionType.USER, "user", "Target user!").setRequired(false)
        );
    }

    @Override @SneakyThrows
    public void onCommand(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();

        OptionMapping actOPT = event.getOption("action");
        if (actOPT == null) {
            reply(event, showInvites(member.getUser()));
            return;
        }
        String action = actOPT.getAsString();
        switch (action.toLowerCase()) {
            case "show" -> {
                OptionMapping targetOPT = event.getOption("user");
                if (targetOPT == null) {
                    reply(event, showInvites(member.getUser()));
                    return;
                }

                if (!perms.has(member, guild, Nodes.INVISTES_SHOW_OTHERS).get(10, TimeUnit.SECONDS)) {
                    errorMessage(event, "You don't have permission to do that!");
                    return;
                }

                User target = targetOPT.getAsUser();
                reply(event, showInvites(target));
                return;
            }
        }
        errorMessage(event, "Unknown action!");
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
