package fi.natroutter.foxbot.data;

import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;

public class Embeds {

    public static EmbedBuilder rules() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("⚖️ Law and order of FoxBox ⚖️");
        eb.setDescription(
                "**§ 1.** _Treat everyone with respect._\n" +
                "\n" +
                "**§ 2.** _No offensive, profane, harassing, defamatory, inappropriate, racist, sexist, homophobic, threatening, infringing, obscene, or unlawful material._\n" +
                "\n" +
                "**§ 3.** _No inappropriate avatars or usernames (which includes anything sexual, offensive, or containing hate speech). Use of excessive unicode characters in your username is not allowed ._\n" +
                "\n" +
                "**§ 4.** _All content and messages on this server are to be in English or Finnish, with the exception of common words or phrases at staff discretion._\n" +
                "\n" +
                "**§ 5.** _Nsfw content only in <#749095674301251654> this doesn't include private channels_\n" +
                "\n" +
                "**§ 6.** _Do not violate the privacy of any other individual or entity._\n" +
                "\n" +
                "**§ 7.** _No bugs, exploits, glitches, hacks, bugs, etc._\n" +
                "\n" +
                "**§ 8.** _No ban evasion._\n" +
                "\n" +
                "**§ 9.** _No voice chat channel hopping._\n" +
                "\n" +
                "**§ 10.** _No annoying, loud or high pitch noises._\n" +
                "\n" +
                "**§ 11.** _No spam. This includes multiple mentions._\n" +
                "\n" +
                "**§ 12.** _No posting of viruses, corrupted files, or any other similar software._\n" +
                "\n" +
                "**§ 13.** _No advertising Discord servers, other programs, websites, or services._\n" +
                "\n" +
                "**§ 14.** _No exploiting possible loopholes in the rules (please report them)._\n" +
                "\n" +
                "**§ 15.** _Only post food releated content in <#660132784039264316>_"
        );
        return eb;
    }

    public static EmbedBuilder general() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("General Information");
        eb.setDescription(
                "\uD83E\uDD16 **Music bot info**\n" +
                "_All music bot commands needs to be executed in <#988810850569707521>_\n" +
                "\n" +
                "\uD83C\uDFAE**Minecraft Info**\n" +
                "_I'm also hosting a whitelisted Minecraft survival server that everyone can join. If you want to join you need to make an application about why you should be whitelisted you need to be trustworthy and ready to play the same map for a long time. To make an application click the button below. If you dont get answare to your application in next 3 days you havent been accepted!_"
        );
        return eb;
    }

    public static EmbedBuilder links() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Links");
        eb.setDescription(
                "\uD83D\uDCD6 **General:**\n" +
                "• Invite Link: _https://discord.gg/qCqXqe4aRK_\n" +
                "• Uptime: _https://uptime.nat.gg/_\n" +
                "\n" +
                "\uD83D\uDC68\u200D\uD83D\uDCBB **Developement:**\n" +
                "• Jenkins: _https://hub.nat.gg/jenkins_\n" +
                "• Nexus: _https://hub.nat.gg/nexus_\n" +
                "• Plugins: _https://plugins.nat.gg/_\n" +
                "\n" +
                "\uD83C\uDF0D **Websites:**\n" +
                "• Personal: _https://NATroutter.fi/_\n" +
                "• Roskis: _https://roskis.net/_\n" +
                "• Caverns: _https://caverns.cc/_\n" +
                "\n" +
                "\uD83C\uDFAE **Games:**\n" +
                "• Survival Map: _https://map.natroutter.fi/_"
        );
        return eb;
    }

    public static EmbedBuilder musicBotUsage() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("MusicBot Usage");
        eb.setDescription(
                "\uD83D\uDCFB **!play <title|URL|subcommand>**\n" +
                "• Plays the provided song\n" +
                "\n" +
                "\uD83D\uDCFB **!nowplaying**\n" +
                "• Shows the song that is currently playing\n" +
                "\n" +
                "\uD83D\uDCFB **!playlists**\n" +
                "• Shows the available playlists\n" +
                "\n" +
                "\uD83D\uDCFB **!skip**\n" +
                "• Votes to skip the current song\n" +
                "\n" +
                "\uD83D\uDCFB **!help**\n" +
                "• Shows list of commands that can be used!"
        );
        return eb;
    }

}
