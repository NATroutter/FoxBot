package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionHandler;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionRewards;
import fi.natroutter.foxbot.feature.voicesessions.listeners.VoiceSessionButtonListener;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fi.natroutter.foxbot.feature.voicesessions.VoiceSessionImageRenderer.formatDuration;

public class VoiceStats extends DiscordCommand {

    private final PermissionHandler perms = FoxBot.getPermissionHandler();
    private final VoiceSessionImageRenderer renderer = new VoiceSessionImageRenderer();

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
                        .addChoice("info", "info"),
                new OptionData(OptionType.CHANNEL, "channel", "Which voice channel to look at").setRequired(false)
                        .setChannelTypes(ChannelType.VOICE, ChannelType.STAGE)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        String action = optionString(event, "action", "top");

        // Discord cannot tie one option to another option's value, so the pairing is checked here.
        if (event.getOption("channel") != null && !action.equals("current")) {
            replyError(event, "That option does not belong here",
                    "`channel:` only means something for a session that is running right now.\n"
                            + "> `/vstats action:current channel:<channel>`");
            return;
        }

        switch (action) {
            case "current" -> showCurrent(event);
            case "info" -> showInfo(event);
            default -> showTop(event);
        }
    }

    /**
     * The session running in one voice channel: the one the caller is sitting in, or the one they
     * named. There is nothing sensible to show for someone who is in neither, so that is an error
     * rather than a silent fall back to some other channel's session.
     */
    private void showCurrent(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || event.getGuild() == null) {
            replyError(event, "This command can only be used in a server.");
            return;
        }

        AudioChannel target = targetChannel(event, member);
        if (target == null) {
            replyError(event, "You are not in a voice channel",
                    "Join one, or name the channel you mean:\n> `/vstats action:current channel:<channel>`");
            return;
        }

        VoiceSessionEntry session = FoxBot.getVoiceSessionHandler().activeSnapshot(target.getIdLong());
        if (session == null) {
            replyInfo(event, "Nothing going on there",
                    "There is no voice session running in " + target.getAsMention() + " right now.");
            return;
        }

        respondWith(event, Nodes.VSTATS, hook ->
                VoiceSessionButtonListener.renderSession(hook, session, 0, renderer));
    }

    /** The named channel if one was given, otherwise whichever the caller is connected to. */
    private static AudioChannel targetChannel(SlashCommandInteractionEvent event, Member member) {
        OptionMapping option = event.getOption("channel");
        if (option != null) {
            GuildChannelUnion channel = option.getAsChannel();
            return channel instanceof AudioChannel audio ? audio : null;
        }

        GuildVoiceState state = member.getVoiceState();
        return state == null ? null : state.getChannel();
    }

    private void showTop(SlashCommandInteractionEvent event) {
        respondWith(event, Nodes.VSTATS_TOP, hook ->
                VoiceSessionButtonListener.renderLeaderboard(hook, event.getGuild().getIdLong(), 0, renderer));
    }

    /**
     * Checks the permission, then hands a deferred reply to {@code render} on the worker thread.
     *
     * <p>Ephemeral: stats are for whoever asked, not the whole channel. Button edits inherit this,
     * so every follow-up page and session view stays private too.
     */
    private void respondWith(SlashCommandInteractionEvent event, Nodes node, Consumer<InteractionHook> render) {
        Member member = event.getMember();
        if (member == null || event.getGuild() == null) {
            replyError(event, "This command can only be used in a server.");
            return;
        }

        try {
            if (!perms.has(member, event.getGuild(), node).get(10, TimeUnit.SECONDS)) {
                replyError(event, "You don't have permission to do that!");
                return;
            }
        } catch (Exception e) {
            replyError(event, "Permission check failed.");
            return;
        }

        event.deferReply(true).queue(hook -> VoiceSessionHandler.worker().execute(() -> render.accept(hook)));
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
                + "leaves — everyone's time in between is counted, even if they come and go.\n"

                + "## 💻  Commands\n"
                + "> ### ● **/vstats top**\n"
                + "> *The longest sessions ever recorded here, best first. One that is still going shows up as **● Live**.*\n"
                + "> \n"
                + "> *The **#1 – #10** buttons open a session and show everyone who was in it.*\n"
                + "> _**Previous** and **Next** page through the top " + VoiceSessionButtonListener.TOP_LIMIT + "_"

                + "\n \n"
                + "> ### ● **/vstats current**\n"
                + "> *The session in the voice channel you are in, with who is in it and how long each of them has been there.*\n"
                + "> \n"
                + "> *`channel:` looks at another voice channel instead.*\n"
                + "> _**Update** redraws it with fresh numbers, once every " + cooldown + "_\n"

                + "## 🏆  Session rewards\n"
                + "> When a session ends, its top three are paid in social credits:\n"
                + "> 🥇 **" + VoiceSessionRewards.baseCredits(1) + "**"
                + "  ·  🥈 **" + VoiceSessionRewards.baseCredits(2) + "**"
                + "  ·  🥉 **" + VoiceSessionRewards.baseCredits(3) + "**\n"
                + "> …multiplied by every whole hour the session lasted, with no cap. "
                + "A five hour session pays 🥇 **" + VoiceSessionRewards.baseCredits(1) * 5 + "**.\n"
                + "> The session has to reach **" + minSession + "**, and you need **" + minPlace + "** in it to place.\n"
                + "> The result is posted in the voice channel's own chat.\n"

                + "## ⏱️  Good to know\n"
                + "> Sessions under **" + formatDuration(VoiceSessionHandler.MIN_DURATION_SECONDS) + "** are not saved at all.\n"
                + "> Bots are never counted, and these replies are only visible to you.";

        eb.setDescription(description);

        return eb;
    }

    private static String optionString(SlashCommandInteractionEvent event, String name, String fallback) {
        OptionMapping option = event.getOption(name);
        return option == null ? fallback : option.getAsString();
    }
}
