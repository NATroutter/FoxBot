package fi.natroutter.foxbot.commands.party;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.feature.parties.data.PartyChange;
import fi.natroutter.foxbot.feature.parties.data.PartyModals;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogMember;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import fi.natroutter.foxlib.logger.types.LogLevel;
import fi.natroutter.foxlib.logger.types.LogReason;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.List;
import java.util.Optional;

public class PartyRenameCommand extends DiscordCommand {

    private final PartyHandler partyHandler = FoxBot.getPartyHandler();
    private final MongoHandler mongo = FoxBot.getMongo();
    private final FoxLogger logger = FoxBot.getLogger();
    private final PermissionHandler permissions = FoxBot.getPermissionHandler();

    public PartyRenameCommand() {
        super("party-rename");
        this.setDescription("Create a new party");
        this.setPermission(Nodes.PARTY_VOICE);
        this.setCooldownTime(120);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.STRING, "name", "New name for your party.").setRequired(true)
                        .setMaxLength(25)
                        .setMinLength(1)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null) {return;}
        if (member == null) {return; }


        String newName = Optional.ofNullable(event.getOption("name")).map(OptionMapping::getAsString).orElse("");

        if (newName.isBlank()) {
            logger.warn("Failed to rename party!",
                    new LogMember(member),
                    new LogReason("Missing name")
            );
            replyError(event, "Invalid Name!", "You must provide a valid name for your party. Please try again with a different name.");
            return;
        }
        if (partyHandler.isBlacklisted(newName)) {
            replyError(event, "Inappropriate Name!", "The provided name contains words that are not allowed. Please choose a different name.");
            logger.warn("Failed to rename party channel",
                    new LogMember(member),
                    new LogData("NewName", newName),
                    new LogReason("Name contains blacklisted or inappropriate words")
            );
            return;
        }

        if (newName.length() > 25) {
            replyError(event, "Too Long name!", "Name can not be longer than 25 characters.");
            logger.warn("Failed to rename party channel",
                    new LogMember(member),
                    new LogData("NewName", (newName.length() >= 100 ? newName.substring(0, 99) + "..." : newName)),
                    new LogReason("Name is longer than 25 characters")
            );
            return;
        }

        if (!partyHandler.isSafeName(newName)) {
            replyError(event, "Name contains illegal characters!", "The name contains one or more illegal characters.\nPlease use only letters, numbers, and the following symbols:\n``- _ . , + * ! # % & / = ? \\ ( ) { } [ ] @ £ $ € ^ ~ < > |``");
            logger.warn("Failed to rename party channel!",
                    new LogMember(member),
                    new LogData("NewName", newName),
                    new LogReason("name contains illegal characters")
            );
            return;
        }


        mongo.getParties().findByID(member.getId(), party -> {

            String oldName = party.getName();

            if (party.getName().equalsIgnoreCase(newName)) {
                replyWarn(event, "No Change Detected!", "The new name is the same as the current name. No changes were made.");
                logger.warn("Failed to rename party channel!",
                        new LogMember(member),
                        new LogData("OldName", oldName),
                        new LogData("NewName", newName),
                        new LogReason("New name is the same as old name")
                );
                return;
            }

            party.setName(newName);
            mongo.save(party);

            replySuccess(event, "Party Renamed Successfully!", "The channel name has been updated.");

            if (party.getChannelID() == 0) return;
            VoiceChannel voice = guild.getVoiceChannelById(party.getChannelID());

            if (voice != null) {
                voice.getManager().setName(newName).queue(s-> {
                    partyHandler.updatePanel(guild, voice, party);
                });
            }
        });

    }
}
