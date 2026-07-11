package fi.natroutter.foxbot.commands.sticker;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.StickerProvider;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StickerAutocompleteListener extends ListenerAdapter {

    private final StickerProvider stickers = FoxBot.getStickerProvider();
    private volatile Set<String> cachedStickerNames = Set.of();
    private volatile List<CachedSticker> cachedStickers = List.of();

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("sticker")) {
            return;
        }
        if (event.getFocusedOption().getName().equalsIgnoreCase("reply")) {
            replyReplyAutocomplete(event);
            return;
        }
        if (!event.getFocusedOption().getName().equalsIgnoreCase("name")) {
            return;
        }

        replyNameAutocomplete(event);
    }

    private void replyNameAutocomplete(CommandAutoCompleteInteractionEvent event) {
        String value = event.getFocusedOption().getValue();
        Optional<String> replyMessageId = StickerReply.messageId(value);
        String query = StickerReply.withoutMarker(value).toLowerCase(Locale.ROOT);
        List<Command.Choice> choices = getCachedStickers().stream()
                .filter(sticker -> sticker.searchName().contains(query))
                .map(sticker -> sticker.choice(replyMessageId.orElse(null)))
                .filter(Objects::nonNull)
                .limit(OptionData.MAX_CHOICES)
                .toList();

        event.replyChoices(choices).queue();
    }

    private void replyReplyAutocomplete(CommandAutoCompleteInteractionEvent event) {
        String query = event.getFocusedOption().getValue().trim();
        Optional<String> pendingMessageId = StickerReply.pendingMessageId(event.getUser().getId());
        if (pendingMessageId.isEmpty()) {
            event.replyChoices(List.of()).queue();
            return;
        }

        String messageId = pendingMessageId.get();
        String marker = StickerReply.marker(messageId);
        if (!query.isEmpty() && !messageId.contains(query) && !marker.contains(query.toLowerCase(Locale.ROOT))) {
            event.replyChoices(List.of()).queue();
            return;
        }

        event.replyChoices(List.of(new Command.Choice(messageId, messageId))).queue();
    }

    private List<CachedSticker> getCachedStickers() {
        Set<String> stickerNames = getStickerNames();
        if (stickerNames.equals(cachedStickerNames)) {
            return cachedStickers;
        }

        synchronized (this) {
            stickerNames = getStickerNames();
            if (stickerNames.equals(cachedStickerNames)) {
                return cachedStickers;
            }

            cachedStickerNames = stickerNames;
            cachedStickers = stickerNames.stream()
                    .sorted()
                    .map(name -> new CachedSticker(
                            name.toLowerCase(Locale.ROOT),
                            new Command.Choice(name, name)
                    ))
                    .toList();

            return cachedStickers;
        }
    }

    private Set<String> getStickerNames() {
        return stickers.get().keySet().stream()
                .filter(name -> name.length() <= OptionData.MAX_CHOICE_NAME_LENGTH)
                .collect(Collectors.toUnmodifiableSet());
    }

    private record CachedSticker(String searchName, Command.Choice choice) {
        private Command.Choice choice(String replyMessageId) {
            if (replyMessageId == null) {
                return choice;
            }

            String value = StickerReply.withMarker(choice.getAsString(), replyMessageId);
            if (value.length() > Command.Choice.MAX_STRING_VALUE_LENGTH) {
                return null;
            }

            String name = choice.getName() + " (reply)";
            if (name.length() > Command.Choice.MAX_NAME_LENGTH) {
                name = choice.getName();
            }

            return new Command.Choice(name, value);
        }
    }
}
