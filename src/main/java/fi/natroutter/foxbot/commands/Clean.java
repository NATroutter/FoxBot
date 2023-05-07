package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.Handlers.NATLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class Clean extends BaseCommand {

    private NATLogger logger = FoxBot.getLogger();

    public Clean() {
        super("clean");
        this.setDescription("Clears x amount of chat history!");
        this.setDeleteDelay(5);
        this.setPermission(Node.CLEAN);
        this.addArguments(
                new OptionData(OptionType.INTEGER, "amount", "This is amount of message you want to delete").setRequired(true).setRequiredRange(1,100)
        );
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        long amount = getOption(args, "amount").getAsLong();

        List<Message> messages = channel.getHistory().retrievePast((int)amount).complete();
        for(Message message : messages) {
            if (message.isEphemeral() || message.isPinned()) {
                continue;
            }
            message.delete().reason("Cleaning chat").queue();

            if (message.getMember() != null){
                User user = message.getMember().getUser();
                logger.warn("Deleting message send by \""+user.getName()+"("+user.getId()+")\" from channel \"" + channel.getName() + "("+channel.getId()+")\"");
            }

        }

        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Chat Cleaning!");
        eb.setDescription("Chat has been cleaned!");
        eb.addField("Requested by", member.getAsMention(), true);
        eb.addField("Amount", String.valueOf(amount), true);
        eb.addField("Channel", channel.getName(), true);

        return eb;
    }
}
