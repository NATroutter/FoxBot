package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.configs.EmbedProvider;
import fi.natroutter.foxbot.configs.data.EmbedData;
import fi.natroutter.foxbot.data.Embeds;
import fi.natroutter.foxbot.database.MongoHandler;
import fi.natroutter.foxbot.database.models.GeneralEntry;
import fi.natroutter.foxbot.handlers.permissions.Node;
import fi.natroutter.foxbot.objects.*;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.data.Modals;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Embed extends BaseCommand {

    private EmbedProvider embeds = FoxBot.getEmbeds();

    public Embed() {
        super("embed");
        this.setDescription("Send embeds files defined in bot configs");
        this.setPermission(Node.EMBED);
        this.setDeleteDelay(30);

        this.addArguments(
                new OptionData(OptionType.STRING, "file", "What file you want to send!")
                        .addChoices(getChoices())
                        .setRequired(false),
                new OptionData(OptionType.STRING, "base64", "Send embed decoded from base64")
                        .setRequired(false)
        );
    }

    public List<Command.Choice> getChoices() {
        List<Command.Choice> choices = new ArrayList<>();
        embeds.get().forEach((name,data)->{
            choices.add(new Command.Choice(name,name));
        });
        return choices;
    }

    private MongoHandler mongo = FoxBot.getMongo();

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {
        EmbedData embed = null;

        OptionMapping optBase64 = getOption(args, "base64");
        if (optBase64 != null) {

            //Load embed from base64
            EmbedProvider.ParseData data = embeds.parseData(optBase64.getAsString(), true);
            if (data.embed() == null) {
                return error(data.message());
            }
            embed = data.embed();

        } else {
            //Load embed from file
            OptionMapping optFile = getOption(args, "file");
            if (optFile == null) {
                return error("Files is not selected!");
            }
            embed = embeds.get().get(optFile.getAsString());
            if (embed == null) {
                return error("That File doesn't exists!");
            }
        }

        MessageEmbed em = embed.asEmbed();
        if (em == null) {
            return error("Invalid embed");
        }

        channel.sendMessageEmbeds(embed.asEmbed()).queue();

        return info("Embed has been send!");
    }
}
