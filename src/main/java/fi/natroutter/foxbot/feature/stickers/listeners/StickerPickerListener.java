package fi.natroutter.foxbot.feature.stickers.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.StickerProvider;
import fi.natroutter.foxbot.feature.stickers.StickerPickerSessionStore;
import fi.natroutter.foxbot.feature.stickers.StickerPreviewCache;
import fi.natroutter.foxbot.feature.stickers.StickerResizer;
import fi.natroutter.foxbot.feature.stickers.data.StickerDescriptor;
import fi.natroutter.foxbot.feature.stickers.data.StickerPickerSession;
import fi.natroutter.foxbot.feature.stickers.data.StickerSize;
import fi.natroutter.foxlib.FoxLib;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StickerPickerListener extends ListenerAdapter {

    public static final int PAGE_SIZE = 4;

    private static final Logger log = LoggerFactory.getLogger(StickerPickerListener.class);
    private static final String PREFIX = "sticker-picker";
    private static final StickerProvider stickers = FoxBot.getStickerProvider();
    private static final StickerPreviewCache previewCache = new StickerPreviewCache();
    private static final StickerPickerSessionStore sessions = new StickerPickerSessionStore();
    private static final StickerResizer resizer = new StickerResizer();
    private static final ExecutorService previewExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "sticker-preview-renderer");
        thread.setDaemon(true);
        return thread;
    });

    public static void rebuildPreviewAssets() throws IOException {
        stickers.reload();
        previewCache.rebuildAssets(orderedStickers());
    }

    public static void openPicker(SlashCommandInteractionEvent event) {
        List<StickerDescriptor> orderedStickers = orderedStickers();
        if (orderedStickers.isEmpty()) {
            event.reply("No sticker files found in the stickers folder.").setEphemeral(true).queue();
            return;
        }

        StickerPickerSession session = sessions.create(
                event.getUser().getIdLong(),
                event.getChannelIdLong(),
                orderedStickers.stream().map(StickerDescriptor::id).toList()
        );

        event.deferReply(true).queue(hook -> previewExecutor.execute(() -> {
            try {
                hook.editOriginalComponents(buildContainer(session))
                        .useComponentsV2()
                        .setReplace(true)
                        .queue();
            } catch (IOException e) {
                sessions.remove(session.id());
                log.warn("Failed to open sticker picker", e);
                hook.editOriginal("Sticker picker could not be rendered.").queue();
            }
        }));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        ParsedId parsed = parse(event.getComponentId());
        if (parsed == null) {
            return;
        }

        Optional<StickerPickerSession> optSession = sessions.find(parsed.sessionId());
        if (optSession.isEmpty()) {
            event.reply("This sticker picker expired. Run /sticker to open a new one.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        StickerPickerSession session = optSession.get();
        if (session.userId() != event.getUser().getIdLong()) {
            event.reply("This sticker picker belongs to another user. Run /sticker to open your own.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        switch (parsed.action()) {
            case "select" -> selectSticker(event, session, parsed.value());
            case "page" -> changePage(event, session, parsed.value());
            case "cancel" -> cancel(event, session);
            default -> {
            }
        }
    }

    private void selectSticker(ButtonInteractionEvent event, StickerPickerSession session, String value) {
        int position = parseInt(value, -1);
        if (position < 0 || position >= PAGE_SIZE) {
            event.reply("That sticker selection is invalid.").setEphemeral(true).queue();
            return;
        }

        int stickerIndex = session.currentPage() * PAGE_SIZE + position;
        if (stickerIndex >= session.stickerIds().size()) {
            event.reply("That sticker is no longer available on this page.").setEphemeral(true).queue();
            return;
        }

        String stickerId = session.stickerIds().get(stickerIndex);
        File sticker = getSticker(stickerId);
        if (sticker == null) {
            sessions.remove(session.id());
            event.editComponents(closedContainer("That sticker was removed. Run /sticker to open a new picker."))
                    .useComponentsV2()
                    .setReplace(true)
                    .queue();
            return;
        }

        event.deferEdit().queue(hook -> {
            FileUpload upload;
            try {
                upload = resizer.resize(sticker, StickerSize.NORMAL);
            } catch (IOException e) {
                log.warn("Sticker picker failed to resize {}", sticker.getAbsolutePath(), e);
                hook.editOriginalComponents(closedContainer("Sticker could not be resized."))
                        .useComponentsV2()
                        .setReplace(true)
                        .queue();
                return;
            }

            MessageEmbed embed = getStickerEmbed(event, upload.getName());
            event.getChannel().sendFiles(upload)
                    .addEmbeds(embed)
                    .setAllowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                    .queue(sent -> {
                        sessions.remove(session.id());
                        hook.deleteOriginal().queue(null, error ->
                                hook.editOriginalComponents(closedContainer("Sticker sent: " + stickerId))
                                        .useComponentsV2()
                                        .setReplace(true)
                                        .queue()
                        );
                    }, error -> {
                        closeQuietly(upload);
                        hook.editOriginalComponents(closedContainer("Sticker could not be sent: " + error.getMessage()))
                                .useComponentsV2()
                                .setReplace(true)
                                .queue();
                    });
        });
    }

    private void changePage(ButtonInteractionEvent event, StickerPickerSession session, String value) {
        int targetPage = parseInt(value, session.currentPage());
        session.setCurrentPage(targetPage);

        event.deferEdit().queue(hook -> previewExecutor.execute(() -> {
            try {
                hook.editOriginalComponents(buildContainer(session))
                        .useComponentsV2()
                        .setReplace(true)
                        .queue();
            } catch (IOException e) {
                log.warn("Failed to render sticker picker page", e);
                hook.editOriginalComponents(closedContainer("Sticker picker page could not be rendered."))
                        .useComponentsV2()
                        .setReplace(true)
                        .queue();
            }
        }));
    }

    private void cancel(ButtonInteractionEvent event, StickerPickerSession session) {
        sessions.remove(session.id());
        event.deferEdit().queue(hook ->
                hook.deleteOriginal().queue(null, error ->
                        hook.editOriginalComponents(closedContainer("Sticker picker unavailable."))
                                .useComponentsV2()
                                .setReplace(true)
                                .queue()
                )
        );
    }

    private static Container buildContainer(StickerPickerSession session) throws IOException {
        List<StickerDescriptor> pageStickers = pageStickers(session);

        List<ContainerChildComponent> components = new ArrayList<>();
        components.add(TextDisplay.of("### Choose a sticker - Page " + (session.currentPage() + 1) + " of " + session.totalPages()));
        components.add(MediaGallery.of(previewItem(session)));
        components.add(selectionRow(session, pageStickers));
        components.add(navigationRow(session));

        return Container.of(components).withAccentColor(new Color(0xF97316));
    }

    private static MediaGalleryItem previewItem(StickerPickerSession session) throws IOException {
        return MediaGalleryItem.fromUrl(previewCache.urlForPage(session.currentPage()))
                .withDescription("Sticker picker preview page");
    }

    private static MessageTopLevelComponent closedContainer(String message) {
        return Container.of(TextDisplay.of(message)).withAccentColor(new Color(0x6B7280));
    }

    private static ActionRow selectionRow(StickerPickerSession session, List<StickerDescriptor> pageStickers) {
        List<Button> buttons = new ArrayList<>();
        for (int index = 0; index < pageStickers.size(); index++) {
            buttons.add(Button.primary(customId(session.id(), "select", String.valueOf(index)), String.valueOf(index + 1)));
        }
        return ActionRow.of(buttons);
    }

    private static ActionRow navigationRow(StickerPickerSession session) {
        int page = session.currentPage();
        int totalPages = session.totalPages();
        return ActionRow.of(
                Button.secondary(customId(session.id(), "page", String.valueOf(page - 1)), "Previous").withDisabled(page == 0),
                Button.secondary(customId(session.id(), "page", String.valueOf(page + 1)), "Next").withDisabled(page >= totalPages - 1),
                Button.danger(customId(session.id(), "cancel", ""), "Cancel")
        );
    }

    private static List<StickerDescriptor> pageStickers(StickerPickerSession session) {
        int start = session.currentPage() * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, session.stickerIds().size());
        List<StickerDescriptor> pageStickers = new ArrayList<>();
        for (int index = start; index < end; index++) {
            String stickerId = session.stickerIds().get(index);
            File file = getSticker(stickerId);
            pageStickers.add(new StickerDescriptor(stickerId, stickerId, file));
        }
        return pageStickers;
    }

    private static List<StickerDescriptor> orderedStickers() {
        return stickers.get().entrySet().stream()
                .map(entry -> new StickerDescriptor(entry.getKey(), entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(sticker -> sticker.name().toLowerCase(Locale.ROOT)))
                .toList();
    }

    private static File getSticker(String name) {
        File sticker = stickers.get(name);
        if (sticker != null) {
            return sticker;
        }
        return stickers.get(FoxLib.getBasename(name));
    }

    private static String customId(String sessionId, String action, String value) {
        if (value == null || value.isEmpty()) {
            return PREFIX + ":" + sessionId + ":" + action;
        }
        return PREFIX + ":" + sessionId + ":" + action + ":" + value;
    }

    private static ParsedId parse(String componentId) {
        String[] parts = componentId.split(":", 4);
        if (parts.length < 3 || !PREFIX.equals(parts[0])) {
            return null;
        }
        return new ParsedId(parts[1], parts[2], parts.length == 4 ? parts[3] : "");
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static MessageEmbed getStickerEmbed(Interaction event, String fileName) {
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(getAuthorName(event), null, getAuthorAvatarUrl(event))
                .setImage("attachment://" + fileName);

        Member member = event.getMember();
        Color color = member != null ? member.getColors().getPrimary() : null;
        if (color != null) {
            embed.setColor(color);
        }

        return embed.build();
    }

    private static String getAuthorName(Interaction event) {
        Member member = event.getMember();
        if (member != null) {
            return member.getEffectiveName();
        }
        return event.getUser().getName();
    }

    private static String getAuthorAvatarUrl(Interaction event) {
        Member member = event.getMember();
        if (member != null) {
            return member.getEffectiveAvatarUrl();
        }
        return event.getUser().getEffectiveAvatarUrl();
    }

    private static void closeQuietly(FileUpload upload) {
        try {
            upload.close();
        } catch (IOException ignored) {
        }
    }

    private record ParsedId(String sessionId, String action, String value) {
    }
}
