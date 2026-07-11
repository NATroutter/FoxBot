package fi.natroutter.foxbot.commands.sticker;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.StickerProvider;
import fi.natroutter.foxbot.permissions.Nodes;
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
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sticker extends DiscordCommand {

    private static final int MAX_STICKER_LIST_LENGTH = 3500;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Pattern PINGABLE_MENTION = Pattern.compile("<@!?\\d+>|<@&\\d+>");

    private final StickerProvider stickers = FoxBot.getStickerProvider();
    private final StickerResizer resizer = new StickerResizer();

    public Sticker() {
        super("sticker");
        this.setDescription("Send special server stickers without paying for nitro");
        this.setPermission(Nodes.STICKER);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "name", "Sticker file name")
                        .setAutoComplete(true)
                        .setRequired(false),
                new OptionData(OptionType.STRING, "size", "Sticker image size")
                        .addChoices(StickerSize.choices())
                        .setRequired(false),
                new OptionData(OptionType.STRING, "message", "Message to send with the sticker")
                        .setMaxLength(MAX_MESSAGE_LENGTH)
                        .setRequired(false),
                new OptionData(OptionType.STRING, "reply", "Message ID to reply to")
                        .setAutoComplete(true)
                        .setMinLength(1)
                        .setMaxLength(32)
                        .setRequired(false)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        OptionMapping optName = event.getOption("name");
        if (optName == null) {
            StickerPickerListener.openPicker(event);
            return;
        }

        stickers.reload();

        String rawName = optName.getAsString();
        String name = StickerReply.withoutMarker(rawName);
        OptionMapping optReply = event.getOption("reply");
        String replyMessageId = getReplyMessageId(optReply, rawName);
        if (optReply != null && replyMessageId == null) {
            replyError(event, "Invalid reply message ID!", "Use a Discord message ID, for example `reply:123456789012345678`.");
            return;
        }
        if (name.isEmpty()) {
            replyError(event, "Sticker name is missing!", "Pick a sticker from autocomplete.");
            return;
        }

        File sticker = getSticker(name);
        StickerSize size = getSize(event.getOption("size"));

        if (sticker == null) {
            replyError(event, "Sticker doesn't exist!");
            return;
        }

        try {
            FileUpload upload = resizer.resize(sticker, size);
            MessageEmbed embed = getStickerEmbed(event, event.getOption("message"), upload.getName());
            if (replyMessageId != null) {
                sendStickerReply(event, replyMessageId, upload, embed, event.getOption("message"));
                return;
            }

            var reply = event.replyFiles(upload)
                    .addEmbeds(embed)
                    .setAllowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE));

            String mentionContent = getMentionContent(event.getOption("message"));
            if (mentionContent != null) {
                reply.setContent(mentionContent);
            }

            reply.queue();
        } catch (IOException e) {
            replyError(event, "Sticker could not be resized!", e.getMessage());
        }
    }

    private String getReplyMessageId(OptionMapping optReply, String rawName) {
        if (optReply != null) {
            return StickerReply.optionMessageId(optReply.getAsString()).orElse(null);
        }
        return StickerReply.messageId(rawName).orElse(null);
    }

    private void sendStickerReply(SlashCommandInteractionEvent event, String replyMessageId, FileUpload upload, MessageEmbed embed, OptionMapping optMessage) {
        event.deferReply(true).queue(hook ->
                event.getChannel().retrieveMessageById(replyMessageId).queue(target -> {
                    var reply = target.replyFiles(upload)
                            .addEmbeds(embed)
                            .setAllowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                            .mentionRepliedUser(false);

                    String mentionContent = getMentionContent(optMessage);
                    if (mentionContent != null) {
                        reply.setContent(mentionContent);
                    }

                    reply.queue(
                            sent -> {
                                StickerReply.clear(event.getUser().getId(), replyMessageId);
                                hook.deleteOriginal().queue(null, ignored -> {});
                            },
                            error -> {
                                closeQuietly(upload);
                                hook.editOriginal("Sticker reply could not be sent: " + error.getMessage()).queue();
                            }
                    );
                }, error -> {
                    closeQuietly(upload);
                    hook.editOriginal("Reply target could not be found in this channel.").queue();
                }),
                error -> closeQuietly(upload));
    }

    private void closeQuietly(FileUpload upload) {
        try {
            upload.close();
        } catch (IOException ignored) {
        }
    }

    private File getSticker(String name) {
        File sticker = stickers.get(name);
        if (sticker != null) {
            return sticker;
        }
        return stickers.get(FoxLib.getBasename(name));
    }

    private String availableStickers() {
        if (stickers.get().isEmpty()) {
            return "No sticker files found in the stickers folder.";
        }

        List<String> names = stickers.get().keySet().stream()
                .sorted()
                .toList();

        StringBuilder list = new StringBuilder()
                .append("Found ")
                .append(names.size())
                .append(" sticker")
                .append(names.size() == 1 ? "" : "s")
                .append(":\n```text\n");

        int shown = 0;
        for (String name : names) {
            String line = "- " + name + "\n";
            if (list.length() + line.length() + "```".length() > MAX_STICKER_LIST_LENGTH) {
                break;
            }
            list.append(line);
            shown++;
        }

        if (shown < names.size()) {
            list.append("... and ")
                    .append(names.size() - shown)
                    .append(" more\n");
        }

        return list.append("```").toString();
    }

    private StickerSize getSize(OptionMapping optSize) {
        if (optSize == null) {
            return StickerSize.NORMAL;
        }
        return StickerSize.fromKey(optSize.getAsString());
    }

    private MessageEmbed getStickerEmbed(SlashCommandInteractionEvent event, OptionMapping optMessage, String fileName) {
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(getAuthorName(event), null, getAuthorAvatarUrl(event))
                .setImage("attachment://" + fileName);

        String message = getMessage(optMessage);
        if (message != null && !containsOnlyMentions(message)) {
            embed.setDescription(message);
        }

        Member member = event.getMember();
        Color color = member != null ? member.getColors().getPrimary() : null;
        if (color != null) {
            embed.setColor(color);
        }

        return embed.build();
    }

    private String getMessage(OptionMapping optMessage) {
        if (optMessage == null) {
            return null;
        }

        String message = optMessage.getAsString().trim();
        if (message.isEmpty()) {
            return null;
        }
        return message;
    }

    private String getMentionContent(OptionMapping optMessage) {
        String message = getMessage(optMessage);
        if (message == null) {
            return null;
        }

        Matcher matcher = PINGABLE_MENTION.matcher(message);
        Set<String> mentions = new LinkedHashSet<>();
        while (matcher.find()) {
            mentions.add(matcher.group());
        }

        if (mentions.isEmpty()) {
            return null;
        }
        return String.join(" ", mentions);
    }

    private boolean containsOnlyMentions(String message) {
        String withoutMentions = PINGABLE_MENTION.matcher(message).replaceAll("").trim();
        return withoutMentions.isEmpty();
    }

    private String getAuthorName(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member != null) {
            return member.getEffectiveName();
        }
        return event.getUser().getName();
    }

    private String getAuthorAvatarUrl(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member != null) {
            return member.getEffectiveAvatarUrl();
        }
        return event.getUser().getEffectiveAvatarUrl();
    }
}
