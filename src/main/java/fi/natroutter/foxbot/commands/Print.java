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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.util.List;

public class Print extends DiscordCommand {

    //the API's own ceilings for an image it fetches, so a refusal can be explained before we send one
    private static final long MAX_REMOTE_BYTES = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    private final BotHandler bot = FoxBot.getBotHandler();
    private final Config.FenPos fenpos = FoxBot.getConfigProvider().get().getFenpos();
    private final FenPosClient client = new FenPosClient(fenpos);

    public Print() {
        super("print");
        this.setDescription("Send a message or an image to the receipt printer");
        this.setPermission(Nodes.PRINT);
        this.setCooldownTime(fenpos.getCooldown());
        this.allowCooldownBypass(false); //paper is finite, nobody gets to skip this one
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "message", "Message to print")
                        .setMaxLength(fenpos.getMaxLength())
                        .setRequired(false),
                new OptionData(OptionType.ATTACHMENT, "image", "PNG or JPEG image to print")
                        .setRequired(false)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        OptionMapping messageOption = event.getOption("message");
        OptionMapping imageOption = event.getOption("image");

        if (messageOption == null && imageOption == null) {
            replyError(event, "Give me a message, an image, or both!");
            return;
        }

        String message = null;
        if (messageOption != null) {
            message = messageOption.getAsString();

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
        }

        String imageUrl = null;
        if (imageOption != null) {
            Message.Attachment image = imageOption.getAsAttachment();
            String rejection = checkImage(image);
            if (rejection != null) {
                replyError(event, "That image can't be printed!", rejection);
                return;
            }
            imageUrl = image.getUrl();
        }

        Member member = event.getMember();
        String sender = member != null ? member.getEffectiveName() : event.getUser().getEffectiveName();

        List<String> data = ReceiptBuilder.build(sender, message, imageUrl, fenpos.getImageWidth());

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

    /**
     * Everything the API can refuse an image for that an attachment already tells us, so it comes
     * back as a sentence here instead of a code from the printer. An interlaced PNG is the one it
     * can't see coming, that one stays an "invalid_image" from the API.
     *
     * @return why the image is unprintable, or null when it is fine
     */
    private String checkImage(Message.Attachment image) {
        String contentType = image.getContentType();
        boolean printable = image.isImage() && contentType != null
                && (contentType.startsWith("image/png") || contentType.startsWith("image/jpeg"));
        if (!printable) {
            return "The printer only takes PNG and JPEG images.";
        }

        long maxBytes = maxImageBytes();
        if (image.getSize() > maxBytes) {
            return "Maximum is " + (maxBytes / 1024) + " KiB, yours was " + (image.getSize() / 1024) + " KiB.";
        }

        Integer width = image.getWidth();
        Integer height = image.getHeight();
        if (width != null && height != null && (width > MAX_DIMENSION || height > MAX_DIMENSION)) {
            return "Maximum is " + MAX_DIMENSION + " pixels on each side, yours was " + width + "x" + height + ".";
        }

        if (!ReceiptBuilder.isSafeUrl(image.getUrl())) {
            return "Discord gave that image a link the printer can't be handed.";
        }

        return null;
    }

    /**
     * The API fetches a remote image through a 2 MiB guard, so a config asking for more than that
     * would only move the refusal to the printer. An unset value lands on the same ceiling.
     */
    private long maxImageBytes() {
        long configured = (long) fenpos.getMaxImageSize() * 1024;
        if (configured < 1 || configured > MAX_REMOTE_BYTES) return MAX_REMOTE_BYTES;
        return configured;
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
