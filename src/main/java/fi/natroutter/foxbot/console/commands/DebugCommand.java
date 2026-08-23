package fi.natroutter.foxbot.console.commands;

import fi.natroutter.foxbot.database.models.VoiceSessionEntry;
import fi.natroutter.foxbot.feature.voicesessions.VoiceSessionRewards;
import fi.natroutter.foxframe.bot.DiscordBot;
import fi.natroutter.foxframe.console.ConsoleCommand;
import fi.natroutter.foxframe.console.ConsoleData;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Console-only helpers for looking at things that normally only happen on their own.
 *
 * <p>{@code debug session-payout <channelID>} posts an example payout card into a channel. It
 * renders the same card a finished session posts, but awards nothing — it is there to check how
 * the card looks without waiting half an hour in voice for a real one.
 */
public class DebugCommand extends ConsoleCommand {

    /** Longest sample session. The floor is the real one, so a preview always pays out. */
    private static final long MAX_SAMPLE_SECONDS = 8 * 3600;

    /**
     * Each place's share of the session, as a percentage range. The bands do not overlap, so a
     * randomised podium still comes out in order, and the lowest of them stays clear of the ten
     * minute bar for any session length the preview can pick.
     */
    private static final int[][] SAMPLE_SHARE_BANDS = {{70, 100}, {46, 69}, {34, 45}};

    /** Enough of the member cache to shuffle through without copying a whole large guild. */
    private static final int MEMBER_POOL = 50;

    public DebugCommand() {
        super("debug", "Debug helpers for features that are hard to trigger by hand",
                "debug session-payout <channelID>");
    }

    @Override
    public ConsoleData execute(DiscordBot handler, ConsoleData data, String[] args) {
        if (args.length < 1) {
            println("Usage: " + getUsage());
            return null;
        }

        if (args[0].equalsIgnoreCase("session-payout")) {
            sessionPayout(handler, args);
            return null;
        }

        println("Unknown debug action: " + args[0]);
        println("Usage: " + getUsage());
        return null;
    }

    private void sessionPayout(DiscordBot handler, String[] args) {
        if (args.length < 2) {
            println("Usage: debug session-payout <channelID>");
            return;
        }
        if (!handler.isRunning()) {
            println("Bot is not connected yet!");
            return;
        }

        long channelID;
        try {
            channelID = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            println("Not a channel ID: " + args[1]);
            return;
        }

        GuildMessageChannel channel = handler.getJDA().getChannelById(GuildMessageChannel.class, channelID);
        if (channel == null) {
            println("No channel with ID " + channelID + " that the bot can post in!");
            return;
        }

        VoiceSessionEntry sample = sampleSession(channel);
        new VoiceSessionRewards().preview(channel, sample);
        println("Sent an example session payout to #" + channel.getName() + " (no credits were awarded)");
    }

    /** Randomised end to end, so running it twice shows different lengths, multipliers and people. */
    private VoiceSessionEntry sampleSession(GuildMessageChannel channel) {
        long now = System.currentTimeMillis();
        long duration = ThreadLocalRandom.current()
                .nextLong(VoiceSessionRewards.MIN_SESSION_SECONDS, MAX_SAMPLE_SECONDS);

        VoiceSessionEntry session = new VoiceSessionEntry();
        session.setSessionID("debug-preview");
        session.setGuildID(channel.getGuild().getIdLong());
        session.setChannelID(channel.getIdLong());
        session.setChannelName(channel.getName());
        session.setStartedAt(now - duration * 1000);
        session.setEndedAt(now);
        session.setDurationSeconds(duration);
        session.setActive(false);
        session.setParticipants(sampleParticipants(channel, duration));
        return session;
    }

    /**
     * Real members, so the card previews with real names and avatars: whoever is in the channel
     * first, then anyone else cached for the guild, and the bot itself if it is alone. Both pools
     * are shuffled, so the preview does not show the same three people every time.
     */
    private List<VoiceSessionEntry.VoiceParticipant> sampleParticipants(GuildMessageChannel channel, long duration) {
        List<Member> candidates = new ArrayList<>();
        if (channel instanceof AudioChannel audio) {
            List<Member> inVoice = new ArrayList<>(audio.getMembers());
            Collections.shuffle(inVoice);
            candidates.addAll(inVoice);
        }

        List<Member> pool = new ArrayList<>();
        for (Member member : channel.getGuild().getMembers()) {
            if (pool.size() >= MEMBER_POOL) {
                break;
            }
            if (!member.getUser().isBot() && !candidates.contains(member)) {
                pool.add(member);
            }
        }
        Collections.shuffle(pool);
        candidates.addAll(pool);

        if (candidates.isEmpty()) {
            candidates.add(channel.getGuild().getSelfMember());
        }

        List<VoiceSessionEntry.VoiceParticipant> participants = new ArrayList<>(SAMPLE_SHARE_BANDS.length);
        for (int index = 0; index < SAMPLE_SHARE_BANDS.length; index++) {
            Member member = candidates.get(index % candidates.size());
            int[] band = SAMPLE_SHARE_BANDS[index];
            long share = ThreadLocalRandom.current().nextInt(band[0], band[1] + 1);
            participants.add(new VoiceSessionEntry.VoiceParticipant(
                    member.getId(),
                    member.getEffectiveName(),
                    member.getEffectiveAvatarUrl(),
                    duration * share / 100,
                    List.of()
            ));
        }
        return participants;
    }
}
