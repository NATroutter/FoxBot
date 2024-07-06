package fi.natroutter.foxbot.data;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.ConfigProvider;
import fi.natroutter.foxbot.utilities.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Embeds {

    private static ConfigProvider config = FoxBot.getConfig();

    public static EmbedBuilder rules() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("⚖️ Law and order of Fox's Nest ⚖️");
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
                "**§ 15.** _Only post food releated content in <#"+config.get().getChannels().getFoodStash()+">_\n" +
                "\n" +
                "**§ 16.** _When posting food content to <#"+config.get().getChannels().getFoodStash()+"> the food has to be made by your self or be ordered from some place (no google images!)_\n" +
                "\n" +
                "**§ 17.** _Do not post old pictures of your previous foods in <#"+config.get().getChannels().getFoodStash()+"> pictures needs to be max 2 days old also do not repost same images!_\n"

        );
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }

    public static EmbedBuilder general(Guild guild) {
        TextChannel foodStash = guild.getTextChannelById(config.get().getChannels().getFoodStash());
        String name = foodStash == null ? "Unknown" : foodStash.getName();

        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("General Information");
        eb.setDescription(
                "## \uD83D\uDCAF Social Credits\n" +
                "_This server has a custom social credit system you can earn this credits by sending messages and being active on the server you can also loose this credits by spämming and leaving voice channels too early etc_\n"+
                "```" +
                "Sending a message: +1\n" +
                "Sending a message with attachment: +2\n" +
                "Sending a image in #"+name+" (read rules): +10\n" +
                "Join voice channel and being there for at least "+config.get().getSocialCredits().getMinVoiceTime()+" mins: +2\n" +
                "Every "+config.get().getSocialCredits().getRewardInterval()+" min when joined to voice channel: +5\n" +
                "\n" +
                "Leaving voice channel before "+config.get().getSocialCredits().getMinVoiceTime()+" mins: -5\n" +
                "Spamming: -1\n" +
                "```\n" +
                "## \uD83D\uDCBB Commands\n" +
                "**/social** - _Shows your social credits_\n" +
                "**/social action:top** - _Shows top 10 most social_\n" +
                "**/about** - _Shows information about FoxBot_\n" +
                "**/coinflip** - _Allows you to flip a coin_\n" +
                "**/dice** - _Allows you to throw a 6D dice_\n" +
                "**/fox** - _Shows a random fox picture_\n" +
                "**/pick** - _Allows you to specify question and answers 1-10 then the bot will pick the best option for you_\n" +
                "**/ask** - _Allows you to ask questions and bot will answer yes, no or maybe_\n" +
                " "
        );
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }

    public static EmbedBuilder minecraft() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Minecraft Information");
        eb.setDescription(
                        "## \uD83C\uDFAE Minecraft Server\n" +
                        "_I'm also hosting a whitelisted Minecraft survival server that everyone can join. If you want to join you need to make an application about why you should be whitelisted you need to be trustworthy and ready to play the same map for a long time. To make an application click the button below. If you dont get answare to your application in next 3 days you havent been accepted!_\n" +
                        " "
        );
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }

    public static EmbedBuilder links() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Links");
        eb.setDescription(
                "### Invite:\n" +
                "_https://discord.gg/qCqXqe4aRK_\n" +
                "### General:\n" +
                "\uD83D\uDD17 | _[Uptime](https://uptime.nat.gg/)_\n" +
                "### Developement:\n" +
                "\uD83D\uDD17 | _[Jenkins](https://hub.nat.gg/jenkins)_\n" +
                "\uD83D\uDD17 | _[Nexus](https://hub.nat.gg/nexus)_\n" +
                "\uD83D\uDD17 | _[GitHub](https://github.com/NATroutter)_\n" +
                "### Websites:\n" +
                "\uD83D\uDD17 | _[Personal](https://NATroutter.fi/)_\n" +
                "\uD83D\uDD17 | _[Roskis](https://roskis.net/)_\n" +
                "\uD83D\uDD17 | _[Caverns](https://caverns.cc/)_\n" +
                "### Minecraft:\n" +
                "\uD83D\uDD17 | _[Survival Map](https://map.natroutter.fi/)_"
        );
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }

    public static EmbedBuilder roleSelector() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Game roles!");
        eb.setDescription("Here, you can select game-specific roles that will allow you to receive notifications when someone wants to play certain games!");
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }

    public static EmbedBuilder musicBotUsage() {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("MusicBot");
        eb.setDescription(
                "## \uD83D\uDD16 General Info\n"+
                "_All music bot commands needs to be executed in \n<#988810850569707521>_\n"+
                "## \uD83D\uDCBB Commands / Usage\n" +
                "**!play <title|URL|subcommand>**\n" +
                "• Plays the provided song\n" +
                "\n" +
                "**!nowplaying**\n" +
                "• Shows the song that is currently playing\n" +
                "\n" +
                "**!playlists**\n" +
                "• Shows the available playlists\n" +
                "\n" +
                "**!skip**\n" +
                "• Votes to skip the current song\n" +
                "\n" +
                "**!help**\n" +
                "• Shows list of commands that can be used!"
        );
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }
    public static EmbedBuilder tradeBotUsage(String traderRole) {
        EmbedBuilder eb = Utils.embedBase();
        eb.setTitle("Pokémon TradeBot");
        eb.setDescription(
                "## \uD83D\uDD16 General Info\n" +
                "_In order to use trading bot you will need to have "+traderRole+" role_\n" +
                "_To get that role you need to invite "+config.get().getGeneral().getInviteCountToRole()+" other user to this server when you have at least done that the role will be added to you in next 12 hours_\n" +
                "\n" +
                "_All trading bot commands need to be executed in channel_\n" +
                "_<#988810850569707521>_\n" +
                "## \uD83C\uDFAE Supported games\n" +
                "_Pokémon Scarlet_\n" +
                "_Pokémon Violet_\n" +
                "## \uD83E\uDD16 Bots\n" +
                "There is multiple bots that you can use to trade with depending on what bot you want to use you may need to change some things!\n" +
                "• <@1166803511862894612>\n" +
                "ㅤ • Command prefix ``.``\n" +
                "• <@1153311556348694528>\n" +
                "ㅤ • Command prefix ``$``\n" +
                "## \uD83D\uDCBB Commands / Usage\n" +
                "### General\n"+
                "• **.help** - _Shows all commands and help for this bot_\n" +
                "• **.trade** - _Start traing with the bot_\n" +
                "• **.info** - _Shows information about bot status etc_\n" +
                "• **.lc** - _Checks if .pk9 file is legal (requires an attachment)_\n" +
                "• **.legalize** - _Converts .pk9 file to legal one (requires an attachment)_\n" +
                "• **.queuestatus / .qs** - _Shows your position in trading queue_\n" +
                "• **.queueclear / .qc** - _Remove your self from the traing queue_\n" +
                "### Example trading\n"+
                "you can trade with commands like this\n" +
                "(You can use this website to build your pokemons [genpkm.com](https://genpkm.com/))\n"+
                "```.trade Pikachu @ Master Ball  \n" +
                "Ability: Lightning Rod  \n" +
                "Tera Type: Electric  \n" +
                "EVs: 252 HP / 252 SpA  \n" +
                "Modest Nature  \n" +
                "- Thunderbolt  \n" +
                "- Iron Tail  \n" +
                "- Electro Ball  \n" +
                "- Quick Attack" +
                "\n\n" +
                "You can use OT:, SID: & TID: aswell\n" +
                "to get your name as Original Trainer\n" +
                "```\n" +
                "### How to obtain SID and TID\n"+
                "Simples way to obtain this information's is to start a trade with the bot when you have connected to the trade the bot will send you message containing your TID and SID\n" +
                "\n" +
                "You can also trade with .pk9 files\n" +
                "Simply just drop the file into discord and type ``.trade`` as message\n" +
                "to create/edit pk9 files you can use [PKHeX](https://projectpokemon.org/home/files/file/1-pkhex/)\n" +
                ""
        );
        eb.setFooter("\uD83D\uDD52 Updated: " + new SimpleDateFormat("dd.MM.yyyy - HH:mm").format(new Date()));
        return eb;
    }


}
