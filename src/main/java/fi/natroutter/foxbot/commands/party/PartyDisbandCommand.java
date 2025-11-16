package fi.natroutter.foxbot.commands.party;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PartyDisbandCommand extends DiscordCommand {

    private final PartyHandler partyHandler = FoxBot.getPartyHandler();
    private final MongoHandler mongo = FoxBot.getMongo();
    private final FoxLogger logger = FoxBot.getLogger();

    public PartyDisbandCommand() {
        super("party-disband");
        this.setDescription("Disband your party");
        this.setPermission(Nodes.PARTY_VOICE);
        this.setCooldownTime(120);
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null) {return;}
        if (member == null) {return; }

        Category category = partyHandler.getPartyCategory(guild);
        if (category == null) return;

        mongo.getParties().findByID(member.getId(), party -> {

            VoiceChannel voice = guild.getVoiceChannelById(party.getChannelID());
            if (voice != null) {
                partyHandler.deleteChannel(voice, "User disbanded their party");
                replySuccess(event, "Party Disbanded!","Your party has been disbanded!");
                return;
            }

            replyError(event, "Party Not Found!", "You do not have a party created in this server.");
        });

    }
}
