package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.handlers.permissions.Nodes;
import fi.natroutter.foxbot.objects.Post;
import fi.natroutter.foxbot.objects.Posts;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Jsoup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class Yiff extends BaseCommand {

    public Yiff() {
        super("yiff");
        this.setDescription("Search some random high quality Yiff");
        this.setPermission(Nodes.YIFF);
        this.setHidden(false);
        this.addArguments(
                new OptionData(OptionType.BOOLEAN, "randomize", "Do you want to randomize your result"),
                new OptionData(OptionType.STRING, "query", "Query some specific words"),
                new OptionData(OptionType.STRING, "user", "Posted by user"),
                new OptionData(OptionType.STRING, "favoritedby", "Favorited by"),
                new OptionData(OptionType.INTEGER, "id", "Post with id"),
                new OptionData(OptionType.STRING, "favcount", "Post with favaourite count"),
                new OptionData(OptionType.STRING, "score", "Post with favaourite score"),
                new OptionData(OptionType.STRING, "width", "Posts with a width of x pixels"),
                new OptionData(OptionType.STRING, "height", "Posts with a height of x pixels"),
                new OptionData(OptionType.STRING, "ratio", "Posts with image ratio of x"),

                new OptionData(OptionType.STRING, "duration", "Video duration"),
                new OptionData(OptionType.STRING, "date", "Search post with specific date"),

                new OptionData(OptionType.STRING, "rating", "Posts with specific rating")
                        .addChoice("safe","safe")
                        .addChoice("questionable","questionable")
                        .addChoice("explicit","explicit"),

                new OptionData(OptionType.STRING, "type", "Type of post")
                        .addChoice("JPG","jpg")
                        .addChoice("PNG","png")
                        .addChoice("GIF","gif")
                        .addChoice("WebM","webm"),

                new OptionData(OptionType.STRING, "order", "How to order posts")
                        .addChoice("Oldest to newest","id")
                        .addChoice("Highest score first","score")
                        .addChoice("Most favorites first","favcount")
                        .addChoice("Most tags first","tagcount")
                        .addChoice("Most comments first","comment_count")
                        .addChoice("Posts with the newest comments","comment_bumped")
                        .addChoice("Largest resolution first","mpixels")
                        .addChoice("Largest file size first","filesize")
                        .addChoice("Wide and short to tall and thin","landscape")
                        .addChoice("Sorts by last update sequence","change")
                        .addChoice("Video duration longest to shortest","duration")
                        .addChoice("Orders posts randomly","random")
                        .addChoice("Lowest score first","score_asc")
                        .addChoice("Least favorites first","favcount_asc")
                        .addChoice("Least tags first","tagcount_asc")
                        .addChoice("Least comments first","comment_count_asc")
                        .addChoice("Posts that have not been commented on for the longest time","comment_bumped_asc")
                        .addChoice("Smallest resolution first","mpixels_asc")
                        .addChoice("Smallest file size first","filesize_asc")
                        .addChoice("Tall and thin to wide and short","portrait")
                        .addChoice("Video duration shortest to longest","duration_asc")

        );

    }

    @Override
    public Object onCommand(JDA jda, Member member, Guild guild, MessageChannel channel, List<OptionMapping> args) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        String url = "https://e621.net/posts.json";

        boolean randomize = false;
        String query = null;

        List<String> tags = new ArrayList<>();

        if (args.size() > 0) {

            for (OptionMapping opt : args) {
                if (opt.getName().equalsIgnoreCase("randomize")) {
                    randomize = opt.getAsBoolean();
                    continue;
                }
                if (opt.getName().equalsIgnoreCase("query")) {
                    tags.add(opt.getAsString());
                    query=opt.getAsString();
                    continue;
                }
                tags.add(opt.getName() + ":" + opt.getAsString());
            }

            url = url + "?limit=" + (randomize ? "20" : "1") + "&tags=" + String.join(" ", tags);
        } else {
            url = url + "?limit=1&tags=order:score type:png anthro";
            tags.addAll(List.of("order:score", "type:png", "anthro"));
        }

        try {
            String json = Jsoup.connect(url).ignoreContentType(true).userAgent("FoxBot/1.0 (NATroutter)").execute().body();
            Posts posts = new Gson().fromJson(json, Posts.class);

            Random rand = new Random();
            Post post;
            if (posts.posts.size() > 0) {
                post = randomize ? posts.posts.get(rand.nextInt(posts.posts.size())) : posts.posts.get(0);
            } else {
                return "No results found for your request!";
            }



            //Shitty date formatting XD
            String[] sDate = post.created_at.split("T");
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            Date date1 = sdf1.parse(sDate[0]);

            String[] sDate2 = sDate[1].split("\\.");
            SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
            Date date2 = sdf2.parse(sDate2[0]);

            SimpleDateFormat sdf1Fin = new SimpleDateFormat("dd.MM.yyyy");
            SimpleDateFormat sdf2Fin = new SimpleDateFormat("HH:mm");

            String finalDate = sdf1Fin.format(date1) + " - " + sdf2Fin.format(date2);

            //Checking for webm content
            if (post.file.ext.equalsIgnoreCase("webm")) {
                eb.addField("\uD83D\uDCF9 Full Video:", "_[Watch here]("+post.file.url+")_",true);
                eb.setImage(post.sample.url);
            } else {
                eb.addField("\uD83D\uDDBC Original Image:", "_[View here]("+post.file.url+")_",true);
                eb.setImage(post.file.url);
            }

            List<String> artists = new ArrayList<>();
            for (String artist : post.tags.artist) {
                artists.add("• ["+artist+"](https://e621.net/posts?tags="+artist+")");
            }

            eb.setTitle("❤️ Enjoy your yiff ❤️");
            eb.setDescription("\n**\uD83E\uDDD1\u200D\uD83C\uDFA8 Artists: **\n"+String.join("\n",artists)+"\n");
            eb.addField("\uD83C\uDF0D Search link: ", "[e621.net/posts](https://e621.net/posts?tags=" + String.join("+",tags).replace(":", "%3A") + ")",true);
            eb.addField("\uD83E\uDDD1 Uploader:","["+post.uploader_id+"](https://e621.net/users/"+post.uploader_id+")",true);
            //eb.addField(" ", " ",true);
            eb.addField("\uD83D\uDD52 Created At:", finalDate, true);
            eb.addField("\uD83D\uDCD9 Content ID:", String.valueOf(post.id), true);
            //eb.addField(" ", " ",true);
            eb.addField("\uD83D\uDCC8 Score:", String.valueOf(post.score.total), true);
            eb.addField("\uD83D\uDC4D Favourite:", String.valueOf(post.fav_count), true);
            eb.addField("\uD83D\uDCBB Content Type:", post.file.ext, true);

            eb.addField("\uD83D\uDD00 Randomize:", randomize ? "Enabled" : "Disabled", true);

            if (query != null) {
                eb.addField("\uD83D\uDCD1 Query: ", query, true);
            }

            eb.setFooter("Requested by: " + member.getUser().getGlobalName(), member.getEffectiveAvatarUrl());

            return eb;

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to retrieve yiff from hell!";
        }
    }
}
