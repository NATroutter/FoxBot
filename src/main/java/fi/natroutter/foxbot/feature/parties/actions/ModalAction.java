package fi.natroutter.foxbot.feature.parties.actions;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.feature.parties.PartyHandler;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;


public abstract class ModalAction {

    protected FoxLogger logger = FoxBot.getLogger();
    protected PartyHandler partyHandler = FoxBot.getPartyHandler();
    protected MongoHandler mongo = FoxBot.getMongo();
    protected PermissionHandler permissions = FoxBot.getPermissionHandler();

    protected final String id;
    protected final MessageChannelUnion channel;
    protected final Guild guild;
    protected final Member member;
    protected final ModalInteraction modalInteraction;
    protected final VoiceChannel voice;

    public ModalAction(ModalInteractionEvent event) {
        this.id = event.getModalId();
        this.channel = event.getChannel();
        this.guild = event.getGuild();
        this.member = event.getMember();
        this.modalInteraction = event.getInteraction();
        this.voice =  event.getChannel().asVoiceChannel();;
    }

    protected boolean isValidSnowflake(String id) {
        if (id == null) return false;
        try {
            Long.parseUnsignedLong(id);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected Member findMember(Guild guild, String idOrName) {
        if (isValidSnowflake(idOrName)) {
            return guild.getMemberById(idOrName);
        }
        return guild.getMembersByName(idOrName, true).stream().findFirst().orElse(null);
    }
}
