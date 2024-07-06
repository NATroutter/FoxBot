package fi.natroutter.foxbot.commands;

import fi.natroutter.foxbot.objects.BaseModal;
import fi.natroutter.foxbot.objects.BaseReply;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Tickets {

//    @Override
//    public Object onModalSubmit(Member member, User Bot, Guild guild, MessageChannel channel, BaseModal modal, List<ModalMapping> args) {
//        if (modal.getId().equalsIgnoreCase("minecraft_modal")) {
//            AppliedUsers.add(member.getIdLong());
//
//            String name = args.get(0).getAsString();
//            String old = args.get(1).getAsString();
//            String howlong = args.get(2).getAsString();
//            String why = args.get(3).getAsString();
//            String what = args.get(4).getAsString();
//
//            TextChannel applyChanel = guild.getChannelById(TextChannel.class, "1065934088122413056");
//            if (applyChanel == null) {
//                return error("Failed to send application! contact server staff!");
//            }
//
//            EmbedBuilder eb = Utils.embedBase();
//            eb.setTitle("Whitelist Application");
//            eb.setDescription("\uD83D\uDCD6 **How long have you been playing Minecraft?**\n"+
//                    "_"+howlong+"_\n"+
//                    "\n"+
//                    "\uD83D\uDCD6 **Why you want to be whitelisted?**\n" +
//                    "_"+why+"_\n"+
//                    "\n" +
//                    "\uD83D\uDCD6 **What are you planing to do if you get whitelisted?**\n" +
//                    "_"+what+"_\n"+
//                    "\n\u200E"
//            );
//
//            ZoneId helsinki = ZoneId.of("Europe/Helsinki");
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
//            formatter.withZone(helsinki);
//
//            String joined = member.getTimeJoined().format(formatter);
//            String created = member.getTimeCreated().format(formatter);
//
//            LocalDateTime ldt = LocalDateTime.ofInstant(member.getTimeJoined().toInstant(), helsinki);
//
//            long daysInGuild = Duration.between(ldt, LocalDateTime.now()).toDays();
//
//            eb.addField("\uD83C\uDFAE Minecraft Name:", name, true);
//            eb.addField("\uD83D\uDC74 Age:", old, true);
//            eb.addField("\uD83D\uDCDC Discord Name:", member.getUser().getGlobalName(),true);
//            eb.addField("\uD83D\uDCDD Discord userID:", member.getUser().getId(),true);
//            eb.addField("\uD83D\uDCC5 Guild Join Date:", joined + " ("+daysInGuild+")", true);
//            eb.addField("\uD83D\uDDD3️ Account Created", created, true);
//
//            eb.setFooter("Posted by: NATroutter",member.getEffectiveAvatarUrl());
//            eb.setTimestamp(LocalDateTime.now(helsinki));
//
//            //eb.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(1767238200000L), ZoneId.of("Etc/UTC")));
//
//
//            applyChanel.sendMessageEmbeds(eb.build()).queue();
//
//            logger.info(member.getUser().getGlobalName()+" has posted new whitelist application!");
//
//            return new BaseReply("Thanks for your application!").setHidden(true);
//        }
//        return error("Invalid modal!");
//    }
//
}
