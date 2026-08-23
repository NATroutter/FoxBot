package fi.natroutter.foxbot.feature.voicesessions;

import fi.natroutter.foxbot.FoxBot;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class VoiceSessionTracker extends ListenerAdapter {

    private final VoiceSessionHandler sessions = FoxBot.getVoiceSessionHandler();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot() || event.getMember().getUser().isSystem()) {
            return;
        }

        if (event.getChannelLeft() != null) {
            sessions.left(event.getChannelLeft(), event.getMember());
        }
        if (event.getChannelJoined() != null) {
            sessions.joined(event.getChannelJoined(), event.getMember());
        }
    }
}
