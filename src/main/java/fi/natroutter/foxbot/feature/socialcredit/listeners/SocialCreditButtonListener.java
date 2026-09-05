package fi.natroutter.foxbot.feature.socialcredit.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.UserEntry;
import fi.natroutter.foxbot.feature.socialcredit.SocialCreditImageRenderer;
import fi.natroutter.foxframe.FoxFrame;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Draws and pages the social credit leaderboard.
 *
 * <p>The page number lives in the button's own ID and every press re-reads the standings, so the
 * buttons never expire and keep working after a restart — the same approach the voice session
 * leaderboard uses.
 */
public class SocialCreditButtonListener extends ListenerAdapter {

    private static final String PREFIX = "social";

    /** How many rows a page shows. The board itself is not capped — it pages all the way down. */
    public static final int PAGE_SIZE = 10;

    /** Discord caches attachments by name, so every render gets its own. */
    private static final AtomicLong IMAGE_SEQUENCE = new AtomicLong();

    /**
     * Rendering fetches faces from Discord's CDN the first time it sees them, which is not
     * something an event thread should be waiting on.
     */
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "social-credit-render");
        thread.setDaemon(true);
        return thread;
    });

    /** Stateless apart from the caches inside it, which are shared anyway. */
    private static final SocialCreditImageRenderer RENDERER = new SocialCreditImageRenderer();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length < 3 || !PREFIX.equals(parts[0]) || !"page".equals(parts[1])) {
            return;
        }

        int page = parseInt(parts[2]);
        event.deferEdit().queue(hook -> WORKER.execute(() -> renderPage(hook, event.getJDA(), page)));
    }

    /**
     * Draws the first page into a reply that has already been deferred. The command hands off here
     * so that opening the board and paging it go through exactly the same rendering.
     */
    public static void openBoard(InteractionHook hook, JDA jda) {
        WORKER.execute(() -> renderPage(hook, jda, 0));
    }

    private static void renderPage(InteractionHook hook, JDA jda, int page) {
        try {
            FoxBot.getSocialCreditHandler().top(top -> draw(hook, jda, page, top));
        } catch (Exception e) {
            FoxBot.getLogger().error("Failed to render the social credit leaderboard", e);
            replace(hook, FoxFrame.error("Something went wrong", "The leaderboard could not be drawn."));
        }
    }

    /** Slices the standings to the requested page and draws it. */
    private static void draw(InteractionHook hook, JDA jda, int page, List<UserEntry> top) {
        List<SocialCreditImageRenderer.Standing> all = standings(jda, top);
        if (all.isEmpty()) {
            replace(hook, FoxFrame.info("Nobody yet", "No one has any social credits yet."));
            return;
        }

        int totalPages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        List<SocialCreditImageRenderer.Standing> onPage = all.subList(
                safePage * PAGE_SIZE, Math.min(safePage * PAGE_SIZE + PAGE_SIZE, all.size()));

        try {
            byte[] png = RENDERER.render(onPage);
            String fileName = "social-top-" + IMAGE_SEQUENCE.incrementAndGet() + ".png";
            hook.editOriginalEmbeds(FoxFrame.embedTemplate()
                            .setTitle("Social Credits Leaderboard")
                            .setImage("attachment://" + fileName)
                            .build())
                    .setAttachments(FileUpload.fromData(png, fileName))
                    .setComponents(pageButtons(safePage, totalPages))
                    .queue();
        } catch (IOException e) {
            FoxBot.getLogger().error("Failed to render the social credit leaderboard", e);
            replace(hook, FoxFrame.error("Render failed", "The leaderboard could not be drawn."));
        }
    }

    /** Omitted entirely when there is only one page, so a short board carries no dead controls. */
    private static List<ActionRow> pageButtons(int page, int totalPages) {
        if (totalPages <= 1) {
            return List.of();
        }
        return List.of(ActionRow.of(
                Button.secondary(id(page - 1), "Previous").withDisabled(page == 0),
                Button.secondary(id(page + 1), "Next").withDisabled(page >= totalPages - 1)
        ));
    }

    /**
     * Turns stored balances into rows to draw, skipping bots and anyone the bot can no longer see.
     * Places are numbered over what is left, so the board never shows a gap where a skipped row was.
     */
    private static List<SocialCreditImageRenderer.Standing> standings(JDA jda, List<UserEntry> top) {
        List<SocialCreditImageRenderer.Standing> standings = new ArrayList<>();
        for (UserEntry entry : top) {
            User user = jda.getUserById(entry.getUserID());
            if (user == null || user.isBot()) {
                continue;
            }
            standings.add(new SocialCreditImageRenderer.Standing(
                    standings.size() + 1,
                    user.getEffectiveName(),
                    user.getEffectiveAvatarUrl(),
                    entry.getSocialCredits()
            ));
        }
        return standings;
    }

    /** Replaces the message with a plain embed, dropping the image and buttons that were on it. */
    private static void replace(InteractionHook hook, EmbedBuilder embed) {
        hook.editOriginalEmbeds(embed.build())
                .setAttachments()
                .setComponents(List.of())
                .queue();
    }

    private static String id(int page) {
        return PREFIX + ":page:" + page;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
