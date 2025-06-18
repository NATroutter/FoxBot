package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public class About extends DiscordCommand {

    public About() {
        super("about");
        this.setDescription("Who am I?");
    }

    @Override
    public void onCommand(SlashCommandInteractionEvent event) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        User bot = event.getJDA().getSelfUser();

        eb.setAuthor("About " + bot.getName(), null, bot.getAvatarUrl());
        eb.setDescription("Hello i'm **" + bot.getName() + "**, "
                + "a slave that serves it's master with honor.\n"
                + "my heart is made out of pure [Java](https://www.java.com/en/) and "
                + "my brains functions with [JDA library](https://github.com/DV8FromTheWorld/JDA) version "+ JDAInfo.VERSION+", and\n"
                + "some genius programming made by my master.");

        eb.addField("Version:", FoxBot.getVer(),true);
        eb.addField("Website:", "[Project Website](https://github.com/NATroutter/FoxBot)",true);
        eb.setFooter("Created by: NATroutter || NATroutter.fi", "https://natroutter.fi/images/logo.png");

        reply(event, eb);
    }
}
