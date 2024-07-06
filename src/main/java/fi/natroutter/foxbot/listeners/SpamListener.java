package fi.natroutter.foxbot.listeners;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.CreditHandler;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.handlers.permissions.Permissions;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class SpamListener extends ListenerAdapter {

    private FoxLogger logger = FoxBot.getLogger();
    private CreditHandler credit = FoxBot.getCreditHandler();

    private ExpiringMap<String, Message> lastMessages = ExpiringMap.builder()
            .expiration(2, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .build();

    private ExpiringMap<String, LocalDateTime> lastMessageTimes = ExpiringMap.builder()
            .expiration(2, TimeUnit.MINUTES)
            .expirationPolicy(ExpirationPolicy.ACCESSED)
            .build();

    @Override @SneakyThrows
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot()) return;

        User user = e.getAuthor();
        String userID = e.getAuthor().getId();

        if (e.getChannelType().equals(ChannelType.TEXT) || e.getChannelType().equals(ChannelType.GUILD_PUBLIC_THREAD)) {

            if (e.getChannelType().equals(ChannelType.GUILD_PUBLIC_THREAD) && !(
                    e.getMessage().getContentRaw().length() > 0 ||
                            e.getMessage().getAttachments().size() > 0 ||
                            e.getMessage().getStickers().size() > 0 ||
                            e.getMessage().getEmbeds().size() > 0
            )) return;

            if (e.getMember() == null) return;

            if (Permissions.has(e.getMember(), Node.BYPASS_SPAM).get(10, TimeUnit.SECONDS)) return;

            if (lastMessageTimes.containsKey(userID)) {
                Duration dura = Duration.between(lastMessageTimes.get(userID), LocalDateTime.now());
                if (dura.toSeconds() <= 2) {
                    e.getMessage().delete().queue();
                    logger.warn(user.getGlobalName() + " tried to spam message (Removing 1 social credit) (FLAG: TooFast)");
                    Utils.sendPrivateMessage(user, Utils.error("Rule breaking!","You are sending messages too fast! Please slow down!\nYou have lost 1 social credit!"), "spam_2sec");
                    credit.take(user, 1);
                    return;
                }
            }

            if (lastMessages.containsKey(userID)) {
                if (lastMessages.get(userID).getContentRaw().length() == 0 && e.getMessage().getContentRaw().length() == 0) return;
                if (lastMessages.get(userID).getContentRaw().equalsIgnoreCase(e.getMessage().getContentRaw())) {
                    e.getMessage().delete().queue(success->{
                        logger.warn(user.getGlobalName() + " tried to spam message (Removing 1 social credit) (FLAG: SameMSG)");
                        Utils.sendPrivateMessage(user, Utils.error("Rule breaking!","You have send same message twice in a row this has been flagged as a spam\nYou have lost 1 social credit!"), "spam_sameMSG");
                        credit.take(user, 1);
                    }, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
                    return;
                }
            }

            lastMessageTimes.put(userID, LocalDateTime.now());
            lastMessages.put(e.getAuthor().getId(), e.getMessage());

        }
    }
}
