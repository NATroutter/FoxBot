package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.handlers.permissions.Permissions;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class Wakeup extends BaseCommand {

    private NATLogger logger = FoxBot.getLogger();

    public static List<Long> users = new ArrayList<>();

    public Wakeup() {
        super("Wakeup");
        this.setDescription("Wakeup afk user");
        this.setHidden(true);
        this.setPermission(Node.WAKEUP);

        this.addArguments(
                new OptionData(OptionType.USER, "target", "Wakeup user that is defined/afk")
                        .setRequired(true)
        );

    }
    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        List<VoiceChannel> channels = guild.getVoiceChannels();

        User targetUser = getOption(args,"target").getAsUser();
        if (targetUser == null) {
            logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but target is not valid user!");
            return error("Invalid user!");
        }
        if (targetUser.isBot() || targetUser.isSystem()) {
            logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but target is bot/system user!");
            return error("That user cannot be woken up! (system/bot)");
        }
        Member target = guild.getMemberById(targetUser.getIdLong());
        if (target == null) {
            logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but target is not guild member!");
            return error("Invalid member!");
        }
        if (target.isOwner()) {
            logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but target is a owner!");
            return error("That user cannot be woken up! (owner)");
        }

        try {
            boolean bypass = Permissions.has(target, Node.BYPASS).get(5, TimeUnit.SECONDS);
            if (bypass) {
                logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " has a bypass permissions!");
                return error("That user cannot be woken up! (bypass)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Wakeup bypass permission check has failed!");
            return error("Permission check failed!");
        }

        GuildVoiceState voiceState = target.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but target is not in a voice!");
            return error("That member is not in voice channel!");
        }
        if (!voiceState.isSelfDeafened()) {
            logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but target is not self defined!");
            return error("That member is already wake!");
        }
        for(VoiceChannel chan : channels) {
            logger.error(chan.getName() + " - " + chan.getMembers().size());
            if (chan.getMembers().size() > 0) {
                for (Member chanMember : chan.getMembers()) {
                    if (chanMember.getIdLong() != target.getIdLong()) continue;
                    logger.info(member.getUser().getAsTag() + " Tries to wakeup user " + targetUser.getAsTag());
                    wakeupUser(target, guild, chan);
                    return info("Waking up!");
                }
                break;
            }
        }
        logger.error(member.getUser().getAsTag() + " Tried to wakeup user " + targetUser.getAsTag() + " but bot can't find user/channel!");
        return error("Can't find user!");
    }

    private void wakeupUser(Member target, Guild guild, VoiceChannel originalChannel) {
        VoiceChannel wake1 = guild.getVoiceChannelById(256970821690458112L);
        VoiceChannel wake2 = guild.getVoiceChannelById(256971073269137429L);

        users.add(target.getIdLong());
        try {
            for (int i=0; i<4; i++) {

                if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) continue;
                guild.moveVoiceMember(target, wake1).queue(null,
                        new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)
                );
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    logger.error("Failed to sleep!");
                    e.printStackTrace();
                    break;
                }

                if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) continue;
                guild.moveVoiceMember(target, wake2).queue(null,
                        new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)
                );

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    logger.error("Failed to sleep!");
                    e.printStackTrace();
                    break;
                }
            }
            if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) return;
            guild.moveVoiceMember(target, originalChannel).queue(null,
                    new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)
            );
        } catch (Exception ignored){}
        users.remove(target.getIdLong());

    }
}
