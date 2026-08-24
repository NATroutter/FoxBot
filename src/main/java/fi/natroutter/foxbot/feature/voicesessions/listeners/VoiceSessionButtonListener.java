package fi.natroutter.foxbot.feature.voicesessions.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionHandler;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.cooldown.Cooldown;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The buttons under a /vstats message.
 *
 * <p>Everything a button needs to do its job is written into its own ID — a page number, a session
 * ID — and every press re-reads the data from the tracker or from Mongo. Nothing about an open
 * message is held in memory, so buttons keep working for as long as the message exists: they do not
 * expire, and they survive a restart of the bot.
 */
public class VoiceSessionButtonListener extends ListenerAdapter {

    private static final String PREFIX = "vstats";

    /** How many of the longest sessions the leaderboard pages through. */
    public static final int TOP_LIMIT = 25;

    /**
     * Discord caches attachments by name, so an edited message that reuses a filename can keep
     * showing the previous image. Every render gets its own filename to avoid that.
     */
    private static final AtomicLong IMAGE_SEQUENCE = new AtomicLong();

    /**
     * Long enough that Update cannot be leaned on as a render loop, short enough that a running
     * session visibly moves between presses.
     */
    public static final int UPDATE_COOLDOWN_SECONDS = 120;

    private final VoiceSessionImageRenderer renderer = new VoiceSessionImageRenderer();

    /** Per user, not per message: re-rendering costs the same whichever message asked for it. */
    private final Cooldown<String> updateCooldown = new Cooldown.Builder<String>()
            .setDefaultCooldown(UPDATE_COOLDOWN_SECONDS)
            .setDefaultTimeUnit(TimeUnit.SECONDS)
            .build();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length < 2 || !PREFIX.equals(parts[0])) {
            return;
        }

        String action = parts[1];
        switch (action) {
            case "page", "back" -> showLeaderboard(event, parts.length > 2 ? parseInt(parts[2], 0) : 0);
            case "open" -> showSession(event, parts[2], parts.length > 3 ? parseInt(parts[3], 0) : 0);
            case "update" -> requestUpdate(event, parts[2], parts.length > 3 ? parseInt(parts[3], 0) : 0);
            default -> {
            }
        }
    }

    // -- buttons ---------------------------------------------------------------------------------

    /** Discord allows at most 5 buttons per action row, so a page of 10 spans two rows. */
    private static final int BUTTONS_PER_ROW = 5;

    /**
     * One button per session actually on the page, never padded with disabled placeholders, plus
     * pagination when there is somewhere to page to.
     */
    static List<ActionRow> leaderboardButtons(int page, int totalPages, List<VoiceSessionEntry> onPage) {
        List<ActionRow> rows = new ArrayList<>();

        List<Button> open = new ArrayList<>();
        for (int index = 0; index < onPage.size(); index++) {
            int rank = page * VoiceSessionImageRenderer.TOP_PAGE_SIZE + index + 1;
            open.add(Button.primary(id("open", onPage.get(index).getSessionID(), rank), "#" + rank));
        }
        for (int offset = 0; offset < open.size(); offset += BUTTONS_PER_ROW) {
            rows.add(ActionRow.of(open.subList(offset, Math.min(offset + BUTTONS_PER_ROW, open.size()))));
        }

        if (totalPages > 1) {
            rows.add(ActionRow.of(
                    Button.secondary(id("page", String.valueOf(page - 1)), "Previous").withDisabled(page == 0),
                    Button.secondary(id("page", String.valueOf(page + 1)), "Next").withDisabled(page >= totalPages - 1)
            ));
        }
        return rows;
    }

    /**
     * Update only while the session is running — a finished one cannot change, so refreshing it
     * would do nothing. Back only when the session was opened from the leaderboard, which is what
     * the rank in the button ID says. A finished session reached through {@code current} has
     * neither, and gets no buttons at all.
     */
    static List<ActionRow> sessionButtons(VoiceSessionEntry session, int rank) {
        List<Button> buttons = new ArrayList<>();
        if (session.isActive()) {
            buttons.add(Button.primary(id("update", session.getSessionID(), rank), "Update"));
        }
        if (rank > 0) {
            buttons.add(Button.secondary(id("back", String.valueOf(pageOf(rank))), "Back"));
        }
        return buttons.isEmpty() ? List.of() : List.of(ActionRow.of(buttons));
    }

    /** Which leaderboard page a placing sits on. */
    private static int pageOf(int rank) {
        return Math.max(0, (rank - 1) / VoiceSessionImageRenderer.TOP_PAGE_SIZE);
    }

    private static String id(String action, String value) {
        return PREFIX + ":" + action + ":" + value;
    }

    /** A rank of 0 is left out entirely, which is how "no leaderboard behind this" is encoded. */
    private static String id(String action, String value, int rank) {
        return rank > 0 ? id(action, value) + ":" + rank : id(action, value);
    }

    // -- rendering -------------------------------------------------------------------------------

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

    /** The longest sessions of a guild, capped at what the leaderboard pages through. */
    static void topSessions(long guildID, Consumer<List<VoiceSessionEntry>> result) {
        FoxBot.getMongo().getVoiceSessions().getTopLongest(guildID, TOP_LIMIT, result);
    }

    /**
     * A session by ID, live if it is still running and from storage once it has ended, so a message
     * about a session keeps working across the moment it ends.
     */
    static VoiceSessionEntry findSession(String sessionID) {
        VoiceSessionEntry live = FoxBot.getVoiceSessionHandler().activeSnapshot(sessionID);
        if (live != null) {
            return live;
        }
        VoiceSessionEntry[] stored = {null};
        FoxBot.getMongo().getVoiceSessions().findByID(sessionID, entry -> stored[0] = entry);
        return stored[0];
    }

    /** Draws one page of the leaderboard into an existing message. */
    public static void renderLeaderboard(InteractionHook hook, long guildID, int page, VoiceSessionImageRenderer renderer) {
        topSessions(guildID, sessions -> {
            if (sessions.isEmpty()) {
                editWith(hook, FoxFrame.info("No sessions yet",
                        "No completed voice sessions have been recorded on this server."));
                return;
            }

            int pageSize = VoiceSessionImageRenderer.TOP_PAGE_SIZE;
            int totalPages = Math.max(1, (sessions.size() + pageSize - 1) / pageSize);
            int safePage = Math.max(0, Math.min(page, totalPages - 1));
            List<VoiceSessionEntry> onPage = sessions.subList(
                    safePage * pageSize, Math.min(safePage * pageSize + pageSize, sessions.size()));

            try {
                byte[] png = renderer.renderTop(onPage, safePage, totalPages);
                String fileName = imageFileName("top");
                hook.editOriginalEmbeds(imageEmbed("Voice Sessions Leaderboard", fileName).build())
                        .setAttachments(FileUpload.fromData(png, fileName))
                        .setComponents(leaderboardButtons(safePage, totalPages, onPage))
                        .queue();
            } catch (IOException e) {
                editWith(hook, FoxFrame.error("Render failed", "The leaderboard could not be drawn."));
            }
        });
    }

    /** Draws one session into an existing message. */
    public static void renderSession(InteractionHook hook, VoiceSessionEntry session, int rank,
                              VoiceSessionImageRenderer renderer) {
        try {
            byte[] png = renderer.renderSession(session, rank);
            String fileName = imageFileName("session");
            String title = rank > 0 ? "Voice Session #" + rank : "Voice Session";

            hook.editOriginalEmbeds(imageEmbed(title, fileName).build())
                    .setAttachments(FileUpload.fromData(png, fileName))
                    .setComponents(sessionButtons(session, rank))
                    .queue();
        } catch (IOException e) {
            editWith(hook, FoxFrame.error("Render failed", "That voice session could not be drawn."));
        }
    }

    /** Replaces a message with a plain embed, dropping the image and buttons that were on it. */
    static void editWith(InteractionHook hook, EmbedBuilder embed) {
        hook.editOriginalEmbeds(embed.build())
                .setAttachments()
                .setComponents(List.of())
                .queue();
    }

    // -- actions ---------------------------------------------------------------------------------

    private void showLeaderboard(ButtonInteractionEvent event, int page) {
        Guild guild = event.getGuild();
        if (guild == null) {
            // Leaving an interaction unacknowledged shows "interaction failed", so answer anyway.
            FoxFrame.replyError(event, "The leaderboard is only available in a server.");
            return;
        }
        event.deferEdit().queue(hook -> VoiceSessionHandler.worker()
                .execute(() -> renderLeaderboard(hook, guild.getIdLong(), page, renderer)));
    }

    private void showSession(ButtonInteractionEvent event, String sessionID, int rank) {
        event.deferEdit().queue(hook -> VoiceSessionHandler.worker().execute(() -> {
            VoiceSessionEntry session = findSession(sessionID);
            if (session == null) {
                editWith(hook, FoxFrame.info("Session is gone",
                        "That voice session is no longer available. Run `/vstats` again."));
                return;
            }
            renderSession(hook, session, rank, renderer);
        }));
    }

    /**
     * The Update button. Refreshing costs a render, so it is rate limited per user — but the check
     * is only here, on the button the user presses repeatedly, not on opening or paging.
     */
    private void requestUpdate(ButtonInteractionEvent event, String sessionID, int rank) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        if (member == null || guild == null) {
            showSession(event, sessionID, rank);
            return;
        }

        FoxBot.getPermissionHandler().has(member, guild, Nodes.BYPASS_COOLDOWN_BUTTON,
                () -> showSession(event, sessionID, rank),
                () -> {
                    String userID = member.getId();
                    if (updateCooldown.hasCooldown(userID)) {
                        long remaining = updateCooldown.getCooldown(userID, TimeUnit.SECONDS);
                        FoxFrame.replyError(event, "Slow down!",
                                "You can update this again in **" + remaining + "** second(s).");
                        return;
                    }
                    updateCooldown.setCooldown(userID);
                    showSession(event, sessionID, rank);
                });
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
