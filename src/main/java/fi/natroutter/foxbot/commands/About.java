package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.interfaces.BaseCommand;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

public class About extends BaseCommand {

    public About() {
        super("about");
        this.setDescription("Who am I?");
        this.setHidden(false);
    }

    @Override
    public Object onCommand(Member member, User bot, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = Utils.embedBase();

        eb.setAuthor("About " + bot.getName(), null, bot.getAvatarUrl());
        eb.setDescription("Hello i'm **" + bot.getName() + "**, "
                + "a slave that serves it's master with honor.\n"
                + "my heart is made out of pure [Java](https://www.java.com/en/) and "
                + "my brains uses [JDA library](https://github.com/DV8FromTheWorld/JDA), and\n"
                + "some clever programming made by my master.");

        eb.addField("Version:", FoxBot.getVERSION(),true);
        eb.addField("Website:", "[Project Website](https://github.com/NATroutter/FoxBot)",true);
        eb.setFooter("Created by: NATroutter || NATroutter.fi", "https://natroutter.fi/assets/img/logo.png");


        return eb;
    }
}
