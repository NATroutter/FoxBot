package fi.natroutter.foxbot.commands.party;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class PartyCreateCommand extends DiscordCommand {

    private final PartyHandler partyHandler = FoxBot.getPartyHandler();
    private final MongoHandler mongo = FoxBot.getMongo();
    private final FoxLogger logger = FoxBot.getLogger();

    public PartyCreateCommand() {
        super("party-create");
        this.setDescription("Create a new party");
        this.setPermission(Nodes.PARTY_VOICE);
        this.setCooldownTime(120);
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null) return;
        if (member == null) return;

        Category category = partyHandler.getPartyCategory(guild);
        if (category == null) return;

        mongo.getParties().findByID(member.getId(), party -> {

            //Get members voice state
            GuildVoiceState voiceState = member.getVoiceState();
            boolean inVoice = voiceState != null && voiceState.inAudioChannel();

            //Get members voice channel from database
            VoiceChannel voice = guild.getVoiceChannelById(party.getChannelID());

            //Check if member already has voice channel
            if (voice == null) {

                //Create new party voice for the user
                partyHandler.createNewParty(guild, category, member, channel -> {
                    replySuccess(event, "Party Created!","Your party has been created: "+channel.getAsMention()+(inVoice ? "\nYou will be moved to the party channel shortly." : ""));
                });
                return;
            }

            //User already has party channel!
            //Check if user is connected to voice channel!
            if (inVoice)  {
                guild.moveVoiceMember(member, voice).queue((success -> {
                    replyError(event, "Party already exists!", "You already have a party created in this server.\nYou will be moved to the party voice channel: " + voice.getAsMention());
                }), (err-> {
                    replyError(event, "Party already exists!", "You already have a party created in this server.\nYou can join it by joining the voice channel: " + voice.getAsMention());
                }));
            } else {
                replyError(event, "Party already exists!", "You already have a party created in this server.\nYou can join it by joining the voice channel: " + voice.getAsMention());
            }
        });

    }
}
