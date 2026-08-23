package fi.natroutter.foxbot.commands.vstats;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionHandler;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionRewards;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer.formatDuration;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class VoiceStats extends DiscordCommand {

    private static final Logger log = LoggerFactory.getLogger(VoiceStats.class);

    private final PermissionHandler perms = FoxBot.getPermissionHandler();
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
                        .addChoice("info", "info")
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        String action = optionString(event, "action", "top");
        switch (action) {
            case "current" -> showCurrent(event);
            case "info" -> showInfo(event);
            default -> showTop(event);
        }
    }

    /**
     * What the command tracks and what it pays, built from the values the feature actually runs on
     * rather than repeated by hand — a tuned threshold cannot silently make this page wrong.
     */
    private void showInfo(SlashCommandInteractionEvent event) {
        User bot = event.getJDA().getSelfUser();
        reply(event, infoEmbed().setAuthor("Voice Session Stats", null, bot.getAvatarUrl()));
    }

    static EmbedBuilder infoEmbed() {
        String minSession = formatDuration(VoiceSessionRewards.MIN_SESSION_SECONDS);
        String minPlace = formatDuration(VoiceSessionRewards.MIN_PARTICIPANT_SECONDS);
        String cooldown = formatDuration(VoiceSessionButtonListener.UPDATE_COOLDOWN_SECONDS);

        EmbedBuilder eb = FoxFrame.embedTemplate();
        String description = "I watch every voice channel while there is someone in it. A **session** "
                + "starts when the first person joins an empty channel and ends when the last one "
                + "leaves — everyone's time in between is counted, even if they come and go.\n";

        description += "## \uD83D\uDCBB  Commands\n";

        description += "> ### ● **/vstats top**\n"
                        + "> *The longest sessions ever recorded here, best first. One that is still going shows up as **● Live**.*\n"
                        + "> \n"
                        + "> *The **#1 – #10** buttons open a session and show everyone who was in it.*\n"
                        + "> _**Previous** and **Next** page through the top " + VoiceSessionView.VIEW_LIMIT + "_";

        description += "\n \n";
        description += "> ### ● **/vstats current**\n"
                        + "> *Every session running right now, with who is in it and how long each of them has been there.*\n"
                        + "> \n"
                        + "> _**Update** redraws it with fresh numbers, once every " + cooldown + "_\n";

        description += "## 🏆  Session rewards\n" +
                "> When a session ends, its top three are paid in social credits:\n"
                        + "> 🥇 **" + VoiceSessionRewards.baseCredits(1) + "**"
                        + "  ·  🥈 **" + VoiceSessionRewards.baseCredits(2) + "**"
                        + "  ·  🥉 **" + VoiceSessionRewards.baseCredits(3) + "**\n"
                        + "> …multiplied by every whole hour the session lasted, with no cap. "
                        + "A five hour session pays 🥇 **" + VoiceSessionRewards.baseCredits(1) * 5 + "**.\n"
                        + "> The session has to reach **" + minSession + "**, and you need **" + minPlace + "** in it to place.\n"
                        + "> The result is posted in the voice channel's own chat.\n";

        description += "## ⏱️  Good to know\n" +
                "> Sessions under **" + formatDuration(VoiceSessionHandler.MIN_DURATION_SECONDS) + "** are not saved at all.\n"
                        + "> Bots are never counted, and these replies are only visible to you.";

        eb.setDescription(description);

        return eb;
    }

    private void showTop(SlashCommandInteractionEvent event) {
        openSessionView(event, Nodes.VSTATS_TOP, "Voice Sessions Leaderboard", "No completed voice sessions found.",
                VoiceSessionView.Kind.LEADERBOARD,
                result -> FoxBot.getMongo().getVoiceSessions()
                        .getTopLongest(event.getGuild().getIdLong(), VoiceSessionView.VIEW_LIMIT, result));
    }

    /**
     * Running sessions, each rendered as a full block with its own participant list. The list
     * comes from the tracker's in-memory state, so this never touches Mongo.
     *
     * <p>Pagination only — the blocks already show what a detail view would.
     */
    private void showCurrent(SlashCommandInteractionEvent event) {
        openSessionView(event, Nodes.VSTATS, "Current Voice Sessions", "No voice sessions are currently active.",
                VoiceSessionView.Kind.CURRENT,
                result -> result.accept(VoiceSessionButtonListener.currentSessions(event.getGuild().getIdLong())));
    }

    private void openSessionView(SlashCommandInteractionEvent event, Nodes node, String title, String emptyMessage,
                                 VoiceSessionView.Kind kind, Consumer<Consumer<List<VoiceSessionEntry>>> query) {
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

        // Ephemeral: stats are for whoever asked, not the whole channel. Button edits inherit this,
        // so every follow-up page and detail view stays private too.
        event.deferReply(true).queue(hook -> VoiceSessionHandler.worker().execute(() -> query.accept(sessions -> {
            if (sessions.isEmpty()) {
                hook.editOriginal(emptyMessage).queue();
                return;
            }

            VoiceSessionView view = views.create(guild.getIdLong(), member.getIdLong(), title, kind, sessions);
            try {
                byte[] png = VoiceSessionButtonListener.renderPage(view);
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
