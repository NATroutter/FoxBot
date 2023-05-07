package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.data.Embeds;
import fi.natroutter.foxbot.handlers.GameRoles;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.objects.*;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.data.Modals;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.NATLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Update extends BaseCommand {

    private List<Long> AppliedUsers = new ArrayList<>();

    public NATLogger logger = FoxBot.getLogger();

    public Update() {
        super("update");
        this.setDescription("Update rules and information!");
        this.setPermission(Node.UPDATE);
        this.setDeleteDelay(5);

        this.addArguments(
                new OptionData(OptionType.STRING, "type", "What you want to update")
                        .addChoice("Instructions", "Instructions")
                        .addChoice("Rules", "Rules")
                        .setRequired(true)
        );

        this.addButton(
                new BaseButton("apply_button", Button.primary("apply", "Create Application"))
        );
        this.addModal(
                new BaseModal("minecraft_modal", Modals.minecraftApplication())
        );

        //Build role selector menu
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("role-selector");
        for (GameRole gRole : GameRoles.roles) {
            menuBuilder.addOptions(
                    SelectOption.of(gRole.name(),gRole.tag())
                            .withDescription(gRole.description())
                            .withEmoji(gRole.emoji())
                            .withDefault(false)
            );
        }
        menuBuilder.setMinValues(0);
        menuBuilder.setMaxValues(25);

        this.addStringMenu(
                new BaseStringMenu("roleSelector", menuBuilder.build())
        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        OptionMapping type = getOption(args, "type");
        if (type == null) {return null;}

        switch (type.getAsString().toLowerCase()) {
            case "instructions" -> {
                MessageChannel chan = guild.getChannelById(MessageChannel.class, "988824728682762240");
                if (chan == null) {return error("Invalid Channel!");}
                Utils.removeMessages(chan, 10);
                chan.sendMessageEmbeds(Embeds.general().build()).addActionRow(this.getButton("apply_button")).queue();
                chan.sendMessageEmbeds(Embeds.links().build(), Embeds.musicBotUsage().build()).queue();
                chan.sendMessageEmbeds(Embeds.roleSelector().build()).addActionRow(getStringMenu("roleSelector").getMenu()).queue();
            }
            case "rules" -> {
                MessageChannel chan = guild.getChannelById(MessageChannel.class, "988824599565316146");
                if (chan == null) {return error("Invalid Channel!");}
                Utils.removeMessages(chan, 10);
                chan.sendMessageEmbeds(Embeds.rules().build()).queue();
            }
            default -> {
                return error("Invalid Channel!");
            }
        };
        return info("Channels has been updated!");
    }

    @Override
    public Object onStringMenuSelect(Member member, User Bot, Guild guild, MessageChannel channel, BaseStringMenu menu, List<SelectOption> args) {
        if (menu.getId().equalsIgnoreCase("roleSelector")) {

            ArrayList<GameRole> removeRoles = new ArrayList<>(GameRoles.roles);
            if (args.size() > 0) {
                ArrayList<GameRole> addRoles = new ArrayList<>();
                ArrayList<GameRole> remRoles = new ArrayList<>();

                for (SelectOption arg : args) {
                    GameRole gRole = GameRoles.fromString(guild, arg.getValue());
                    if (gRole == null) {continue;}

                    Role role = GameRoles.getRole(guild, gRole.tag());
                    if (role == null) {continue;}

                    addRoles.add(gRole);
                    guild.addRoleToMember(member, role).queue();
                    removeRoles.stream().filter(r -> r.tag().equalsIgnoreCase(gRole.tag())).findFirst().ifPresent(removeRoles::remove);
                }
                for (GameRole gRole : removeRoles) {
                    Role role = GameRoles.getRole(guild, gRole.tag());
                    if (role == null) {continue;}
                    remRoles.add(gRole);
                    guild.removeRoleFromMember(member, role).queue();
                }

                String added = String.join(", ", addRoles.stream().map(GameRole::tag).toList());
                String removed = String.join(", ", remRoles.stream().map(GameRole::tag).toList());

                logger.info("User " + member.getUser().getAsTag() + " has updated their game roles! Added: (" + added + ") Removed: (" + removed+")");
                return new BaseReply(info("Your roles has been updated!")).setHidden(true).setDeleteDelay(30);
            } else {
                for (GameRole gRole : GameRoles.roles) {
                    Role role = GameRoles.getRole(guild, gRole.tag());
                    if (role == null) {continue;}
                    guild.removeRoleFromMember(member, role).queue();
                }
                logger.info("User " + member.getUser().getAsTag() + " has removed all their game roles!");
                return new BaseReply(info("Your roles has been removed!")).setHidden(true).setDeleteDelay(30);
            }
        }
        return error("Invalid menu!");
    }

    @Override
    public Object onButtonPress(Member member, User Bot, Guild guild, MessageChannel channel, BaseButton button) {

        if (button.getId().equalsIgnoreCase("apply_button")) {
            if (!AppliedUsers.contains(member.getIdLong())) {
                return this.getModal("minecraft_modal");
            }
            return new BaseReply("You have already applied!").setHidden(true);
        }
        return null;
    }

    @Override
    public Object onModalSubmit(Member member, User Bot, Guild guild, MessageChannel channel, BaseModal modal, List<ModalMapping> args) {
        if (modal.getId().equalsIgnoreCase("minecraft_modal")) {
            AppliedUsers.add(member.getIdLong());

            String name = args.get(0).getAsString();
            String old = args.get(1).getAsString();
            String howlong = args.get(2).getAsString();
            String why = args.get(3).getAsString();
            String what = args.get(4).getAsString();

            MessageChannel applyChanel = guild.getChannelById(MessageChannel.class, "1065934088122413056");
            if (applyChanel == null) {
                return error("Failed to send application! contact server staff!");
            }

            EmbedBuilder eb = Utils.embedBase();
            eb.setTitle("Whitelist Application");
            eb.setDescription("\uD83D\uDCD6 **How long have you been playing Minecraft?**\n"+
                    "_"+howlong+"_\n"+
                    "\n"+
                    "\uD83D\uDCD6 **Why you want to be whitelisted?**\n" +
                    "_"+why+"_\n"+
                    "\n" +
                    "\uD83D\uDCD6 **What are you planing to do if you get whitelisted?**\n" +
                    "_"+what+"_\n"+
                    "\n\u200E"
            );

            ZoneId helsinki = ZoneId.of("Europe/Helsinki");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            formatter.withZone(helsinki);

            String joined = member.getTimeJoined().format(formatter);
            String created = member.getTimeCreated().format(formatter);

            LocalDateTime ldt = LocalDateTime.ofInstant(member.getTimeJoined().toInstant(), helsinki);

            long daysInGuild = Duration.between(ldt, LocalDateTime.now()).toDays();

            eb.addField("\uD83C\uDFAE Minecraft Name:", name, true);
            eb.addField("\uD83D\uDC74 Age:", old, true);
            eb.addField("\uD83D\uDCDC Discord Name:", member.getUser().getAsTag(),true);
            eb.addField("\uD83D\uDCDD Discord userID:", member.getUser().getId(),true);
            eb.addField("\uD83D\uDCC5 Guild Join Date:", joined + " ("+daysInGuild+")", true);
            eb.addField("\uD83D\uDDD3️ Account Created", created, true);

            eb.setFooter("Posted by: NATroutter",member.getEffectiveAvatarUrl());
            eb.setTimestamp(LocalDateTime.now(helsinki));

            applyChanel.sendMessageEmbeds(eb.build()).queue();

            logger.info(member.getUser().getAsTag()+" has posted new whitelist application!");

            return new BaseReply("Thanks for your application!").setHidden(true);
        }
        return error("Invalid modal!");
    }
}
