package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.feature.printer.FenPosClient;
import fi.natroutter.foxbot.feature.printer.ReceiptBuilder;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.FoxLib;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class Print extends DiscordCommand {

    private final BotHandler bot = FoxBot.getBotHandler();
    private final Config.FenPos fenpos = FoxBot.getConfigProvider().get().getFenpos();
    private final FenPosClient client = new FenPosClient(fenpos);

    public Print() {
        super("print");
        this.setDescription("Send a message to the receipt printer");
        this.setPermission(Nodes.PRINT);
        this.setCooldownTime(fenpos.getCooldown());
        this.allowCooldownBypass(false); //paper is finite, nobody gets to skip this one
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "message", "Message to print")
                        .setMaxLength(fenpos.getMaxLength())
                        .setRequired(true)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        String message = Objects.requireNonNull(event.getOption("message")).getAsString();

        if (FoxLib.isBlank(message)) {
            replyError(event, "You can't print an empty message!");
            return;
        }
        if (message.length() > fenpos.getMaxLength()) {
            replyError(event, "Message is too long!",
                    "Maximum is " + fenpos.getMaxLength() + " characters, yours was " + message.length() + ".");
            return;
        }
        if (ReceiptBuilder.hasControlCharacters(message)) {
            replyError(event, "Your message contains characters the printer can't accept!");
            return;
        }

        Member member = event.getMember();
        String sender = member != null ? member.getEffectiveName() : event.getUser().getEffectiveName();

        List<String> data = ReceiptBuilder.build(sender, message);

        event.replyEmbeds(printing())
                .setEphemeral(true)
                .queue(hook -> {
                    FenPosClient.Result result = client.submit(data);
                    if (!result.success()) {
                        hook.editOriginalEmbeds(FoxFrame.error(result.error()).build()).queue();
                        return;
                    }
                    hook.editOriginalEmbeds(printed(result)).queue();
                });
    }

    private MessageEmbed printing() {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(new Color(166, 36, 36));
        eb.setTitle("Printing...");
        eb.setDescription("_Your message is on its way to the printer, Please standby_");
        eb.setThumbnail("https://cdn.nat.gg/img/css-loader.gif");
        eb.setTimestamp(FoxFrame.unix());
        eb.setFooter("FoxBot", bot.getJDA().getSelfUser().getAvatarUrl());
        return eb.build();
    }

    private MessageEmbed printed(FenPosClient.Result result) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setColor(new Color(67, 160, 71));
        eb.setTitle("Message queued for printing!");
        eb.setDescription("**Printer:** " + fenpos.getDevice()
                + "\n**Lines:** " + result.lines()
                + "\n**Job:** `" + result.jobId() + "`");
        eb.setThumbnail("https://cdn.nat.gg/img/green_checkmark.png");
        eb.setTimestamp(FoxFrame.unix());
        eb.setFooter("FoxBot", bot.getJDA().getSelfUser().getAvatarUrl());
        return eb.build();
    }

}
