package fi.natroutter.foxbot.commands.party;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.feature.parties.data.RealRegion;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Optional;

public class PartySetRegionCommand extends DiscordCommand {

    private final PartyHandler partyHandler = FoxBot.getPartyHandler();
    private final MongoHandler mongo = FoxBot.getMongo();
    private final FoxLogger logger = FoxBot.getLogger();
    private final PermissionHandler permissions = FoxBot.getPermissionHandler();

    public PartySetRegionCommand() {
        super("party-set-region");
        this.setDescription("Create a new party");
        this.setPermission(Nodes.PARTY_VOICE);
        this.setCooldownTime(120);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "region", "Set channel region for the party").setRequired(true)
                        .addChoices(RealRegion.choices())
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null) {return;}
        if (member == null) {return; }


        RealRegion newRegion = Optional.ofNullable(event.getOption("region")).map(OptionMapping::getAsString).map(RealRegion::fromKeyNullable).orElse(null);

        if (newRegion == null) {
            logger.warn("Failed to find region!",
                    new LogMember(member),
                    new LogReason("Invalid region")
            );
            replyError(event, "Invalid Region!", "You must provide a valid region for your party. Please try again with a different region.");
            return;
        }

        mongo.getParties().findByID(member.getId(), party -> {
            String oldRegion = party.getRegion();

            if (party.getRegion().equalsIgnoreCase(newRegion.getKey())) {
                replyWarn(event, "No Change Detected!", "The new region is the same as the current region. No changes were made.");
                logger.warn("Failed to change party channel region!",
                        new LogMember(member),
                        new LogData("OldRegion", oldRegion),
                        new LogData("NewRegion", newRegion),
                        new LogReason("New region is the same as old region")
                );
                return;
            }

            party.setRegion(newRegion.getKey());
            mongo.save(party);

            replySuccess(event, "Party region changed Successfully!", "The channels region has been updated.");

            if (party.getChannelID() == 0) return;
            VoiceChannel voice = guild.getVoiceChannelById(party.getChannelID());

            if (voice != null) {
                voice.getManager().setRegion(newRegion.toDiscord()).queue(s-> {
                    partyHandler.updatePanel(guild, voice, party);
                });
            }
        });

    }
}
