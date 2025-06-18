package fi.natroutter.foxbot.feature.parties.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class CreatePartyListener extends ListenerAdapter {

    private Config config = FoxBot.getConfig().get();
    private FoxLogger logger = FoxBot.getLogger();
    private PartyHandler partyHandler = FoxBot.getPartyHandler();
    private PermissionHandler permissions = FoxBot.getPermissionHandler();

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {

        long partyChannel = config.getParty().getNewPartyChannel();
        long partyCategory = config.getParty().getPartyCategory();

        AudioChannelUnion joinedChannel = event.getChannelJoined();

        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (joinedChannel != null && joinedChannel.getIdLong() == partyChannel) {

            Category category = guild.getCategoryById(partyCategory);
            if (category == null) {
                logger.error("Party category does not exists or is not setup correctly in configs!");
                return;
            }

            permissions.has(member, guild, Nodes.PARTY_VOICE, ()-> {
                partyHandler.createPartyChannel(guild, category, member);
            }, ()-> {
                guild.kickVoiceMember(member).queue();
                FoxFrame.sendPrivateMessage(
                        member.getUser(),
                        FoxFrame.error("Error while creating new party!", "You do not have permission to make new voice parties!"),
                        "party_voice_no_permissions"
                );
            });
        }
    }
}





























