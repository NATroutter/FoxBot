package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.BotHandler;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxframe.data.logs.LogChannel;
import fi.natroutter.foxframe.data.logs.LogUser;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Prune extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();
    private BotHandler bot = FoxBot.getBotHandler();

    public Prune() {
        super("prune");
        this.setDescription("Prune x amount of chat history!");
        this.setPermission(Nodes.PRUNE);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
                new OptionData(OptionType.INTEGER, "amount", "Amount of message to delete")
                        .setRequired(true)
                        .setRequiredRange(1,100),
                new OptionData(OptionType.STRING, "mode", "Cleaning mode")
                        .setRequired(true)
                        .addChoice("all","all")
                        .addChoice("bot","bot")
        );
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        int amount = Objects.requireNonNull(event.getOption("amount")).getAsInt();
        String mode = Objects.requireNonNull(event.getOption("mode")).getAsString();

        MessageChannel channel = event.getMessageChannel();
        Member member = event.getMember();
        Instant timestamp = FoxFrame.unix();

        EmbedBuilder eb = PruneProgressEmbed(member, channel, timestamp, amount, 0, amount, false);

        event.replyEmbeds(eb.build()).setEphemeral(true).queue(hook-> {

            channel.getHistory().retrievePast(amount).queue(messages-> {

                AtomicInteger deleted = new AtomicInteger();

                for(Message message : messages) {
                    if (message.isEphemeral() || message.isPinned()) continue;
                    if (mode.equalsIgnoreCase("bot")) {
                        if (!message.getAuthor().isBot()) continue;
                    }
                    message.delete().reason("Pruning chat").queue(success ->{
                        deleted.getAndIncrement();
                        if (message.getMember() != null){
                            User user = message.getMember().getUser();
                            logger.warn("Pruning message",
                                    new LogUser(user),
                                    new LogChannel(channel)
                            );
                        }

                        if (deleted.get() != messages.size()) {
                            hook.editOriginalEmbeds(PruneProgressEmbed(member, channel, timestamp, amount, deleted.get(), messages.size(), false).build()).queue();
                        } else {
                            hook.editOriginalEmbeds(PruneProgressEmbed(member, channel, timestamp, amount, deleted.get(), messages.size(), true).build()).queue(finishedMessage-> {
                                finishedMessage.delete().queueAfter(10, TimeUnit.SECONDS);
                            });
                        }

                    });

                }

            });

        });

    }

    private EmbedBuilder PruneProgressEmbed(Member Requester, Channel channel, Instant timestamp,  int requestedAmount, int pruned, int pruneTotal, boolean finished) {
        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Pruning Chat!");

        if (finished) {
            eb.setDescription("Chat has been pruned!");
            eb.setColor(new Color(67, 160, 71));
            eb.setThumbnail("https://cdn.nat.gg/img/green_checkmark.png");
        } else {
            eb.setDescription("Chat pruning in progress...");
            eb.setColor(new Color(166, 129, 36));
            eb.setThumbnail("https://cdn.nat.gg/img/css-loader.gif");
        }

        eb.setTimestamp(timestamp);
        eb.setFooter("FoxBot", bot.getJDA().getSelfUser().getAvatarUrl());
        eb.addField("Requested by", Requester.getAsMention(), true);
        eb.addField("Pruned", pruned + "/" + pruneTotal, true);
        eb.addField("Amount", String.valueOf(requestedAmount), true);
        eb.addField("Channel", channel.getName(), true);
        return eb;
    }



}
