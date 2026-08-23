package fi.natroutter.foxbot.commands.vstats;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionHandler;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class VoiceStats extends DiscordCommand {

    private static final Logger log = LoggerFactory.getLogger(VoiceStats.class);

    private final PermissionHandler perms = FoxBot.getPermissionHandler();
    private final VoiceSessionImageRenderer renderer = new VoiceSessionImageRenderer();
    private final VoiceSessionViewStore views = VoiceSessionViewStore.get();

    public VoiceStats() {
        super("vstats");
        this.setDescription("View voice session stats");
        this.setPermission(Nodes.VSTATS);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "action", "What do you want to see?").setRequired(false)
                        .addChoice("top", "top")
                        .addChoice("current", "current")
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        String action = optionString(event, "action", "top");
        switch (action) {
            case "current" -> showCurrent(event);
            default -> showTop(event);
        }
    }

    private void showTop(SlashCommandInteractionEvent event) {
        openSessionView(event, Nodes.VSTATS_TOP, "Voice Sessions Leaderboard", "No completed voice sessions found.", true,
                result -> FoxBot.getMongo().getVoiceSessions()
                        .getTopLongest(event.getGuild().getIdLong(), VoiceSessionView.VIEW_LIMIT, result));
    }

    /**
     * Running sessions, rendered through the same image flow as the leaderboard. The list comes
     * from the tracker's in-memory state, so this never touches Mongo.
     *
     * <p>Pagination only — no per-session detail buttons.
     */
    private void showCurrent(SlashCommandInteractionEvent event) {
        openSessionView(event, Nodes.VSTATS, "Current Voice Sessions", "No voice sessions are currently active.", false,
                result -> result.accept(FoxBot.getVoiceSessionHandler().activeSnapshots().stream()
                        .filter(session -> session.getGuildID() == event.getGuild().getIdLong())
                        .limit(VoiceSessionView.VIEW_LIMIT)
                        .toList()));
    }

    private void openSessionView(SlashCommandInteractionEvent event, Nodes node, String title, String emptyMessage,
                                 boolean detailsEnabled, Consumer<Consumer<List<VoiceSessionEntry>>> query) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        if (member == null || guild == null) {
            replyError(event, "This command can only be used in a server.");
            return;
        }

        try {
            if (!perms.has(member, guild, node).get(10, TimeUnit.SECONDS)) {
                replyError(event, "You don't have permission to do that!");
                return;
            }
        } catch (Exception e) {
            replyError(event, "Permission check failed.");
            return;
        }

        event.deferReply(false).queue(hook -> VoiceSessionHandler.worker().execute(() -> query.accept(sessions -> {
            if (sessions.isEmpty()) {
                hook.editOriginal(emptyMessage).queue();
                return;
            }

            VoiceSessionView view = views.create(guild.getIdLong(), member.getIdLong(), title, detailsEnabled, sessions);
            try {
                byte[] png = renderer.renderTop(view.pageSessions(), view.currentPage(), view.totalPages());
                String fileName = VoiceSessionButtonListener.imageFileName("top");
                hook.editOriginalEmbeds(VoiceSessionButtonListener.imageEmbed(title, fileName).build())
                        .setAttachments(FileUpload.fromData(png, fileName))
                        .setComponents(VoiceSessionButtonListener.viewButtons(view))
                        .queue();
            } catch (IOException e) {
                views.remove(view.id());
                log.warn("Failed to render voice session leaderboard", e);
                hook.editOriginal("Voice session leaderboard could not be rendered.").queue();
            }
        })));
    }

    private static String optionString(SlashCommandInteractionEvent event, String name, String fallback) {
        OptionMapping option = event.getOption(name);
        return option == null ? fallback : option.getAsString();
    }
}
