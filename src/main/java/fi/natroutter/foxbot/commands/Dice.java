package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.List;
import java.util.Random;

public class Dice extends DiscordCommand {

    //TODO add "public" option (required:false) - Determines is the resulting message hidden or not!

    public Dice() {
        super("dice");
        this.setDescription("Roll a dice");
        this.setPermission(Nodes.DICE);
    }

    public enum DiceSides {
        side1("https://i.imgur.com/klENaSt.png",1),
        side2("https://i.imgur.com/BLPeVCX.png",2),
        side3("https://i.imgur.com/AZsvRsE.png",3),
        side4("https://i.imgur.com/PYIC6xC.png",4),
        side5("https://i.imgur.com/Z8Nlhpn.png",5),
        side6("https://i.imgur.com/EBPTJHE.png",6);

        @Getter private String image;
        @Getter private int number;
        DiceSides(String image, int number) {
            this.image = image;
            this.number = number;
        }

        private static final List<DiceSides> VALUES = List.of(values());
        private static final int SIZE = VALUES.size();
        private static final Random RANDOM = new Random();

        public static DiceSides random() {
            return VALUES.get(RANDOM.nextInt(SIZE));
        }
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Rolling a dice!");

        DiceSides side = DiceSides.random();
        eb.setDescription("**Dice landed on number:**  *`"+side.getNumber()+"`*");
        eb.setThumbnail(side.getImage());

        reply(event, eb, false);
    }
}
