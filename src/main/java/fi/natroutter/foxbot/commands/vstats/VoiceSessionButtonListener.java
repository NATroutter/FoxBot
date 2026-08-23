package fi.natroutter.foxbot.commands.vstats;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionHandler;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxlib.cooldown.Cooldown;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class VoiceSessionButtonListener extends ListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(VoiceSessionButtonListener.class);
    private static final String PREFIX = "vstats";

    /**
     * Discord caches attachments by name, so an edited message that reuses a filename can keep
     * showing the previous image. Every render gets its own filename to avoid that.
     */
    private static final AtomicLong IMAGE_SEQUENCE = new AtomicLong();

    /**
     * Long enough that Update cannot be leaned on as a render loop, short enough that a running
     * session visibly moves between presses.
     */
    static final int UPDATE_COOLDOWN_SECONDS = 120;

    private final VoiceSessionViewStore views = VoiceSessionViewStore.get();
    private final VoiceSessionImageRenderer renderer = new VoiceSessionImageRenderer();

    /** Per user, not per view: re-rendering costs the same whichever message asked for it. */
    private final Cooldown<String> updateCooldown = new Cooldown.Builder<String>()
            .setDefaultCooldown(UPDATE_COOLDOWN_SECONDS)
            .setDefaultTimeUnit(TimeUnit.SECONDS)
            .build();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        ParsedId parsed = parse(event.getComponentId());
        if (parsed == null) {
            return;
        }

        Optional<VoiceSessionView> optView = views.find(parsed.viewId());
        if (optView.isEmpty()) {
            event.reply("This leaderboard expired. Run /vstats again.").setEphemeral(true).queue();
            return;
        }

        VoiceSessionView view = optView.get();
        if (view.userID() != event.getUser().getIdLong()) {
            event.reply("This leaderboard belongs to another user. Run /vstats to open your own.").setEphemeral(true).queue();
            return;
        }

        switch (parsed.action()) {
            case "page" -> changePage(event, view, parseInt(parsed.value(), view.currentPage()));
            case "open" -> openDetail(event, view, parseInt(parsed.value(), -1));
            case "back" -> showLeaderboard(event, view);
            case "update" -> requestUpdate(event, view);
            default -> {
            }
        }
    }

    /** Discord allows at most 5 buttons per action row, so a page of 10 spans two rows. */
    private static final int BUTTONS_PER_ROW = 5;

    /**
     * Only controls that do something.
     *
     * <p>Detail buttons appear one per session actually on the page, never padded with disabled
     * placeholders, and only for views that offer details at all. Pagination is omitted unless
     * there is somewhere to page to, and Update only appears on the live view, where the numbers
     * actually move.
     */
    static List<ActionRow> viewButtons(VoiceSessionView view) {
        List<ActionRow> rows = new ArrayList<>();

        if (view.detailsEnabled()) {
            int start = view.currentPage() * view.pageSize();
            int end = Math.min(start + view.pageSize(), view.sessions().size());

            List<Button> sessionButtons = new ArrayList<>();
            for (int sessionIndex = start; sessionIndex < end; sessionIndex++) {
                sessionButtons.add(Button.primary(customId(view.id(), "open", String.valueOf(sessionIndex)), "#" + (sessionIndex + 1)));
            }
            for (int offset = 0; offset < sessionButtons.size(); offset += BUTTONS_PER_ROW) {
                rows.add(ActionRow.of(sessionButtons.subList(offset, Math.min(offset + BUTTONS_PER_ROW, sessionButtons.size()))));
            }
        }

        int page = view.currentPage();
        int totalPages = view.totalPages();

        List<Button> controls = new ArrayList<>();
        if (totalPages > 1) {
            controls.add(Button.secondary(customId(view.id(), "page", String.valueOf(page - 1)), "Previous").withDisabled(page == 0));
            controls.add(Button.secondary(customId(view.id(), "page", String.valueOf(page + 1)), "Next").withDisabled(page >= totalPages - 1));
        }
        if (view.kind() == VoiceSessionView.Kind.CURRENT) {
            controls.add(Button.primary(customId(view.id(), "update", ""), "Update"));
        }
        if (!controls.isEmpty()) {
            rows.add(ActionRow.of(controls));
        }
        return rows;
    }

    /** The live sessions a CURRENT view shows, capped the same way whoever opened it capped them. */
    static List<VoiceSessionEntry> currentSessions(long guildID) {
        return FoxBot.getVoiceSessionHandler().activeSnapshots(guildID).stream()
                .limit(VoiceSessionView.VIEW_LIMIT)
                .toList();
    }

    static ActionRow detailButtons(VoiceSessionView view) {
        return ActionRow.of(Button.secondary(customId(view.id(), "back", ""), "Back"));
    }

    static EmbedBuilder imageEmbed(String title, String fileName) {
        return new EmbedBuilder()
                .setTitle(title)
                .setColor(new Color(0x5865F2))
                .setImage("attachment://" + fileName);
    }

    /** Unique per render, so an edit never resolves to a cached attachment. */
    static String imageFileName(String kind) {
        return "vstats-" + kind + "-" + IMAGE_SEQUENCE.incrementAndGet() + ".png";
    }

    /** Each view kind has its own layout, so paging must re-render through the matching one. */
    static byte[] renderPage(VoiceSessionView view) throws IOException {
        VoiceSessionImageRenderer renderer = new VoiceSessionImageRenderer();
        return switch (view.kind()) {
            case CURRENT -> renderer.renderCurrent(view.pageSessions());
            case LEADERBOARD -> renderer.renderTop(view.pageSessions(), view.currentPage(), view.totalPages());
        };
    }

    /**
     * Re-polls the tracker and redraws the live view, unless the user is still on cooldown.
     *
     * <p>The bypass node is the same one the framework uses for its own buttons, so anyone who may
     * already spam buttons elsewhere is not stopped here either.
     */
    private void requestUpdate(ButtonInteractionEvent event, VoiceSessionView view) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        if (member == null || guild == null) {
            updateCurrent(event, view);
            return;
        }

        FoxBot.getPermissionHandler().has(member, guild, Nodes.BYPASS_COOLDOWN_BUTTON,
                () -> updateCurrent(event, view),
                () -> {
                    String userID = member.getId();
                    if (updateCooldown.hasCooldown(userID)) {
                        long remaining = updateCooldown.getCooldown(userID, TimeUnit.SECONDS);
                        event.reply("Slow down! You can update again in " + remaining + " second(s).")
                                .setEphemeral(true).queue();
                        return;
                    }
                    updateCooldown.setCooldown(userID);
                    updateCurrent(event, view);
                });
    }

    private void updateCurrent(ButtonInteractionEvent event, VoiceSessionView view) {
        List<VoiceSessionEntry> sessions = currentSessions(view.guildID());
        if (sessions.isEmpty()) {
            // Everyone has left since the view was opened: there is nothing left to draw or click.
            views.remove(view.id());
            event.deferEdit().queue(hook -> hook.editOriginal("No voice sessions are currently active.")
                    .setEmbeds(List.of())
                    .setAttachments()
                    .setComponents(List.of())
                    .queue());
            return;
        }

        view.setSessions(sessions);
        showLeaderboard(event, view);
    }

    private void changePage(ButtonInteractionEvent event, VoiceSessionView view, int page) {
        view.setCurrentPage(page);
        showLeaderboard(event, view);
    }

    private void showLeaderboard(ButtonInteractionEvent event, VoiceSessionView view) {
        view.setDetailIndex(-1);
        event.deferEdit().queue(hook -> VoiceSessionHandler.worker().execute(() -> {
            try {
                byte[] png = renderPage(view);
                String fileName = imageFileName("top");
                hook.editOriginalEmbeds(imageEmbed(view.title(), fileName).build())
                        .setAttachments(FileUpload.fromData(png, fileName))
                        .setComponents(viewButtons(view))
                        .queue();
            } catch (IOException e) {
                log.warn("Failed to render voice session leaderboard", e);
                hook.editOriginal("Voice session leaderboard could not be rendered.").setComponents(List.of()).queue();
            }
        }));
    }

    private void openDetail(ButtonInteractionEvent event, VoiceSessionView view, int sessionIndex) {
        if (sessionIndex < 0 || sessionIndex >= view.sessions().size()) {
            event.reply("That session is not available on this page.").setEphemeral(true).queue();
            return;
        }

        view.setDetailIndex(sessionIndex);
        VoiceSessionEntry session = view.sessions().get(sessionIndex);
        event.deferEdit().queue(hook -> VoiceSessionHandler.worker().execute(() -> {
            try {
                byte[] png = renderer.renderDetail(session, sessionIndex + 1);
                String fileName = imageFileName("detail");
                hook.editOriginalEmbeds(imageEmbed("Voice Session #" + (sessionIndex + 1), fileName).build())
                        .setAttachments(FileUpload.fromData(png, fileName))
                        .setComponents(List.of(detailButtons(view)))
                        .queue();
            } catch (IOException e) {
                log.warn("Failed to render voice session detail", e);
                hook.editOriginal("Voice session detail could not be rendered.").setComponents(List.of()).queue();
            }
        }));
    }

    private static String customId(String viewId, String action, String value) {
        if (value == null || value.isBlank()) {
            return PREFIX + ":" + viewId + ":" + action;
        }
        return PREFIX + ":" + viewId + ":" + action + ":" + value;
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

    private record ParsedId(String viewId, String action, String value) {
    }
}
