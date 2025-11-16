package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.EmbedProvider;
import fi.natroutter.foxbot.configs.data.EmbedData;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.List;

public class Embed extends DiscordCommand {

    private EmbedProvider embeds = FoxBot.getEmbedProvider();

    public Embed() {
        super("embed");
        this.setDescription("Send embeds files defined in bot configs");
        this.setPermission(Nodes.EMBED);
        this.setDeleteDelay(30);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
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

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {
        EmbedData embed = null;

        event.deferReply(true).queue();

        OptionMapping optBase64 = event.getOption("base64");
        if (optBase64 != null) {

            //Load embed from base64
            EmbedProvider.ParseData data = embeds.parseData(optBase64.getAsString(), true);
            if (!data.success()) {
                replyError(event,data.message());
                return;
            }
            embed = data.embed();

        } else {
            //Load embed from file
            OptionMapping optFile = event.getOption("file");
            if (optFile == null) {
                replyError(event, "Files is not selected!");
                return;
            }
            embed = embeds.get().get(optFile.getAsString());
            if (embed == null) {
                replyError(event, "That File doesn't exists!");
                return;
            }
        }

        MessageEmbed em = embed.asEmbed();
        if (em == null) {
            replyError(event,"Invalid embed");
            return;
        }

        MessageChannel channel = event.getMessageChannel();
        channel.sendMessageEmbeds(em).queue();

        event.getHook().editOriginalEmbeds(FoxFrame.info("Embed has been sent!","This message will be deleted in 30 seconds").build()).queue();
    }
}
