package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;
import java.util.Random;

public class CoinFlip extends BaseCommand {

    public CoinFlip() {
        super("coinflip");
        this.setDescription("Flip a coin!");
        this.setHidden(false);
        this.setPermission(Nodes.COINFLIP);
    }

    private String HeadsImage = "https://i.imgur.com/PJM7dHM.png";
    private String TailsImage = "https://i.imgur.com/4wObnrN.png";

    @Override
    public Object onCommand(JDA jda, Member member, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = FoxFrame.embedTemplate();

        eb.setTitle("Coinflip!");

        Random rnd = new Random();
        if (rnd.nextBoolean()) {
            eb.setDescription("**Winning side is:**  _`Heads`_");
            eb.setThumbnail(HeadsImage);
        } else {
            eb.setDescription("**Winning side is:**  _`Tails`_");
            eb.setThumbnail(TailsImage);
        }

        return eb;
    }
}
