package fi.natroutter.foxbot.feature.stickers.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.expiringmap.ExpirationPolicy;
import fi.natroutter.foxlib.expiringmap.ExpiringMap;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StickerReply extends ListenerAdapter {

    public static final String CONTEXT_COMMAND_NAME = "Reply with sticker";

    private static final Pattern REPLY_MARKER = Pattern.compile("(?i)(?:^|\\s)reply:(\\d+)(?=$|\\s)");
    private static final ExpiringMap<String, String> PENDING_REPLIES = ExpiringMap.builder()
            .expiration(10, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.CREATED)
            .build();

    public static String marker(String messageId) {
        return "reply:" + messageId;
    }

    public static Optional<String> messageId(String input) {
        Matcher matcher = REPLY_MARKER.matcher(input);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    public static Optional<String> optionMessageId(String input) {
        String value = input.trim();
        if (value.matches("\\d+")) {
            return Optional.of(value);
        }
        return messageId(value);
    }

    public static String withoutMarker(String input) {
        return REPLY_MARKER.matcher(input).replaceAll(" ").trim();
    }

    public static String withMarker(String stickerName, String messageId) {
        return stickerName + " " + marker(messageId);
    }

    public static void remember(String userId, String messageId) {
        PENDING_REPLIES.put(userId, messageId);
    }

    public static Optional<String> pendingMessageId(String userId) {
        return Optional.ofNullable(PENDING_REPLIES.get(userId));
    }

    public static void clear(String userId, String messageId) {
        PENDING_REPLIES.remove(userId, messageId);
    }

    @Override
    public void onMessageContextInteraction(MessageContextInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(CONTEXT_COMMAND_NAME)) {
            return;
        }

        if (event.getGuild() == null || event.getMember() == null) {
            FoxFrame.replyError(event, "Server only", "This interaction can only be used in a server.", true);
            return;
        }

        FoxBot.getPermissionHandler().has(event.getMember(), event.getGuild(), Nodes.STICKER, () -> {
            String messageId = event.getTarget().getId();
            remember(event.getUser().getId(), messageId);
            event.getGuild().retrieveCommands().queue(commands -> {
                String command = commands.stream()
                        .filter(cmd -> cmd.getType() == Command.Type.SLASH)
                        .filter(cmd -> cmd.getName().equalsIgnoreCase("sticker"))
                        .findFirst()
                        .map(Command::getAsMention)
                        .orElse("`/sticker`");

                FoxFrame.replyInfo(
                        event,
                        "Reply with sticker",
                        "Click " + command + ", pick the stored message from the `reply` autocomplete, and pick a sticker.",
                        true
                );
            }, error -> FoxFrame.replyInfo(
                    event,
                    "Reply with sticker",
                    "Use `/sticker reply:" + messageId + "` and pick a sticker from autocomplete.",
                    true
            ));
        }, () -> FoxFrame.replyError(event, "No permission", "You don't have permissions to use this command!", true));
    }
}
