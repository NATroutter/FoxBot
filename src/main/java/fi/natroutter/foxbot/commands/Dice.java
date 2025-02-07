package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.command.BaseCommand;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;
import java.util.Random;

public class Dice extends BaseCommand {

    public Dice() {
        super("dice");
        this.setDescription("Roll a dice");
        this.setHidden(false);
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
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        eb.setTitle("Rolling a dice!");

        DiceSides side = DiceSides.random();
        eb.setDescription("**Dice landed on number:**  _`"+side.getNumber()+"`_");
        eb.setThumbnail(side.getImage());

        return eb;
    }
}
