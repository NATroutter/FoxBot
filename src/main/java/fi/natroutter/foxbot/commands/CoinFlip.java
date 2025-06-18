package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.Random;

public class CoinFlip extends DiscordCommand {

    //TODO add "public" option (required:false) - Determines is the resulting message hidden or not!

    public CoinFlip() {
        super("coinflip");
        this.setDescription("Flip a coin!");
        this.setPermission(Nodes.COINFLIP);
    }

    private String HeadsImage = "https://i.imgur.com/PJM7dHM.png";
    private String TailsImage = "https://i.imgur.com/4wObnrN.png";

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        EmbedBuilder eb = FoxFrame.embedTemplate();

        eb.setTitle("Coinflip!");

        Random rnd = new Random();
        if (rnd.nextBoolean()) {
            eb.setDescription("**Winning side is:**  *`Heads`*");
            eb.setThumbnail(HeadsImage);
        } else {
            eb.setDescription("**Winning side is:**  *`Tails`*");
            eb.setThumbnail(TailsImage);
        }

        reply(event, eb, false);
    }
}
