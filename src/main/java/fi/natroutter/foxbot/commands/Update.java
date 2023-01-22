package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.data.Embeds;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.objects.BaseButton;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.objects.BaseModal;
import fi.natroutter.foxbot.data.Modals;
import fi.natroutter.foxbot.objects.BaseReply;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.NATLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
        this.setPermission(Nodes.UPDATE);
        this.setDeleteDelay(5);

        this.addArguments(
                new OptionData(OptionType.STRING, "type", "What you want to update")
                        .addChoice("Instructions", "Instructions")
                        .addChoice("Rules", "Rules")
                        .setRequired(true)
        );

        this.addButton(new BaseButton("apply_button", Button.primary("apply", "Create Application")));

        this.addModal(new BaseModal("minecraft_modal", Modals.minecraftApplication()));

    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        String type = args.get(0).getAsString();

        MessageChannel chan = switch (type.toLowerCase()) {
            case "instructions" -> guild.getChannelById(MessageChannel.class, "988824728682762240");
            case "rules" -> guild.getChannelById(MessageChannel.class, "988824599565316146");
            default -> null;
        };
        if (chan == null) {
            return "Invalid Channel!";
        }

        List<Message> messages = chan.getHistory().retrievePast(10).complete();
        for (Message message : messages) {
            if (message.isPinned()) {continue;}
            message.delete().complete();
        }

        if (type.equalsIgnoreCase("instructions")) {
            chan.sendMessageEmbeds(Embeds.general().build()).addActionRow(this.getButton("apply_button")).queue();
            chan.sendMessageEmbeds(Embeds.links().build(), Embeds.musicBotUsage().build()).queue();
        }

        if (type.equalsIgnoreCase("rules")) {
            chan.sendMessageEmbeds(Embeds.rules().build()).queue();
        }

        return "Channels has been updated!";
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
                return "Failed to send application! contact server staff!";
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
        return "Invalid modal!";
    }
}
