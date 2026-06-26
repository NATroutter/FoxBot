package fi.natroutter.foxbot.commands.sticker;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.StickerProvider;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Locale;

public class StickerAutocompleteListener extends ListenerAdapter {

    private final StickerProvider stickers = FoxBot.getStickerProvider();

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase("sticker")) {
            return;
        }
        if (!event.getFocusedOption().getName().equalsIgnoreCase("name")) {
            return;
        }

        stickers.reload();

        String query = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
        List<Command.Choice> choices = stickers.get().keySet().stream()
                .sorted()
                .filter(name -> name.toLowerCase(Locale.ROOT).contains(query))
                .filter(name -> name.length() <= OptionData.MAX_CHOICE_NAME_LENGTH)
                .limit(OptionData.MAX_CHOICES)
                .map(name -> new Command.Choice(name, name))
                .toList();

        event.replyChoices(choices).queue();
    }
}
