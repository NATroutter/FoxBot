package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.permissions.PermissionHandler;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Wakeup extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private Config config = FoxBot.getConfigProvider().get();
    private PermissionHandler perms = FoxBot.getPermissionHandler();

    public static List<Long> users = new ArrayList<>();

    public static boolean isWaking = false;

    public Wakeup() {
        super("Wakeup");
        this.setDescription("Wakeup afk user");
        this.setPermission(Nodes.WAKEUP);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.USER, "target", "Wakeup user that is defined/afk")
                        .setRequired(true)
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        String globalName = member.getUser().getGlobalName();

        User targetUser = Objects.requireNonNull(event.getOption("target")).getAsUser();

        List<VoiceChannel> channels = guild.getVoiceChannels();

        if (targetUser.isBot() || targetUser.isSystem()) {
            logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is bot/system user!");
            replyError(event, "That user cannot be woken up! (system/bot)");
            return;
        }
        Member target = guild.getMemberById(targetUser.getIdLong());
        if (target == null) {
            logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not guild member!");
            replyError(event, "Invalid member!");
            return;
        }
        if (target.isOwner()) {
            logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is a owner!");
            replyError(event, "That user cannot be woken up! (owner)");
            return;
        }

        try {
            boolean bypass = perms.has(target, guild, Nodes.BYPASS_WAKEUP).get(5, TimeUnit.SECONDS);
            if (bypass) {
                logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " has a bypass permissions!");
                replyError(event, "That user cannot be woken up! (bypass)");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Wakeup bypass permission check has failed!");
            replyError(event, "Permission check failed!");
            return;
        }

        GuildVoiceState voiceState = target.getVoiceState();
        if (voiceState == null || !voiceState.inAudioChannel()) {
            logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not in a voice!");
            replyError(event, "That member is not in voice channel!");
            return;
        }
        if (!voiceState.isSelfDeafened()) {
            logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but target is not self defined!");
            replyError(event, "That member is already wake!");
            return;
        }
        for(VoiceChannel chan : channels) {
            if (!chan.getMembers().isEmpty()) {
                for (Member chanMember : chan.getMembers()) {
                    if (chanMember.getIdLong() != target.getIdLong()) continue;

                    if (isWaking) {
                        logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but bot is already waking up someone!");
                        replyError(event, "Bot is already waking up someone!");
                        return;
                    }

                    logger.info(globalName + " Tries to wakeup user " + targetUser.getGlobalName());
                    wakeupUser(target, guild, chan);
                    replyError(event, "Waking up!");
                    return;
                }
                break;
            }
        }
        logger.error(globalName + " Tried to wakeup user " + targetUser.getGlobalName() + " but bot can't find user/channel!");
        replyError(event, "Can't find user!");
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
