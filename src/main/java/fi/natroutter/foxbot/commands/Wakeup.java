package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.handlers.permissions.PermissionHandler;
import fi.natroutter.foxframe.command.BaseCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
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
import java.util.concurrent.TimeUnit;

public class Wakeup extends BaseCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private Config config = FoxBot.getConfig().get();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public static List<Long> users = new ArrayList<>();

    public static boolean isWaking = false;

    public Wakeup() {
        super("Wakeup");
        this.setDescription("Wakeup afk user");
        this.setHidden(true);
        this.setPermission(Nodes.WAKEUP);

        this.addArguments(
                new OptionData(OptionType.USER, "target", "Wakeup user that is defined/afk")
                        .setRequired(true)
        );

    }
    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        List<VoiceChannel> channels = guild.getVoiceChannels();

        User targetUser = getOption(args,"target").getAsUser();
        String memberTag = member.getUser().getGlobalName();

        if (targetUser == null) {
            logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not valid user!");
            return error("Invalid user!");
        }
        if (targetUser.isBot() || targetUser.isSystem()) {
            logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is bot/system user!");
            return error("That user cannot be woken up! (system/bot)");
        }
        Member target = guild.getMemberById(targetUser.getIdLong());
        if (target == null) {
            logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not guild member!");
            return error("Invalid member!");
        }
        if (target.isOwner()) {
            logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is a owner!");
            return error("That user cannot be woken up! (owner)");
        }

        try {
            boolean bypass = perms.has(target, Nodes.BYPASS).get(5, TimeUnit.SECONDS);
            if (bypass) {
                logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " has a bypass permissions!");
                return error("That user cannot be woken up! (bypass)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Wakeup bypass permission check has failed!");
            return error("Permission check failed!");
        }

        GuildVoiceState voiceState = target.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not in a voice!");
            return error("That member is not in voice channel!");
        }
        if (!voiceState.isSelfDeafened()) {
            logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not self defined!");
            return error("That member is already wake!");
        }
        for(VoiceChannel chan : channels) {
            if (chan.getMembers().size() > 0) {
                for (Member chanMember : chan.getMembers()) {
                    if (chanMember.getIdLong() != target.getIdLong()) continue;

                    if (isWaking) {
                        logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but bot is already waking up someone!");
                        return error("Bot is already waking up someone!");
                    }

                    logger.info(memberTag + " Tries to wakeup user " + targetUser.getGlobalName());
                    wakeupUser(target, guild, chan);
                    return info("Waking up!");
                }
                break;
            }
        }
        logger.error(memberTag + " Tried to wakeup user " + targetUser.getGlobalName() + " but bot can't find user/channel!");
        return error("Can't find user!");
    }

    private void wakeupUser(Member target, Guild guild, VoiceChannel originalChannel) {
        VoiceChannel wake1 = guild.getVoiceChannelById(config.getChannels().getWakeup1());
        VoiceChannel wake2 = guild.getVoiceChannelById(config.getChannels().getWakeup2());

        new Thread(() -> {
            users.add(target.getIdLong());
            isWaking = true;
            try {
                for (int i=0; i<4; i++) {

                    if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) continue;
                    guild.moveVoiceMember(target, wake1).queue(null,
                            new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)
                    );
                    Thread.sleep(500);

                    if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) continue;
                    guild.moveVoiceMember(target, wake2).queue(null,
                            new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)
                    );
                    Thread.sleep(500);
                }
                if (target.getVoiceState() == null || !target.getVoiceState().inAudioChannel()) return;
                guild.moveVoiceMember(target, originalChannel).queue(null,
                        new ErrorHandler().ignore(ErrorResponse.USER_NOT_CONNECTED)
                );
                Thread.sleep(500);
            } catch (Exception ignored){}
            isWaking = false;
            users.remove(target.getIdLong());
        }).start();

    }
}
