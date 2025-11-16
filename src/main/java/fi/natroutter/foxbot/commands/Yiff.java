package fi.natroutter.foxbot.commands;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.permissions.Nodes;
import fi.natroutter.foxbot.data.E621Post;
import fi.natroutter.foxbot.data.E621PostCollection;
import fi.natroutter.foxframe.FoxFrame;
import fi.natroutter.foxframe.bot.command.DiscordCommand;
import fi.natroutter.foxlib.logger.FoxLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jsoup.Jsoup;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Yiff extends DiscordCommand {

    private FoxLogger logger = FoxBot.getLogger();

    public Yiff() {
        super("yiff");
        this.setDescription("Search some random high quality Yiff");
        this.setPermission(Nodes.YIFF);
    }

    @Override
    public List<OptionData> options() {
        return List.of(
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
    public void onCommand(SlashCommandInteractionEvent event) {

        EmbedBuilder eb = FoxFrame.embedTemplate();
        String url = "https://e621.net/posts.json";

        boolean randomize = false;
        String query = null;

        Member member = event.getMember();
        User user = member.getUser();
        String globalName = user.getGlobalName();

        List<String> tags = new ArrayList<>();

        if (!event.getOptions().isEmpty()) {

            for (OptionMapping opt : event.getOptions()) {
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
            E621PostCollection posts = new Gson().fromJson(json, E621PostCollection.class);

            Random rand = new Random();
            E621Post post;
            if (!posts.posts.isEmpty()) {
                post = randomize ? posts.posts.get(rand.nextInt(posts.posts.size())) : posts.posts.get(0);
            } else {
                replyError(event, "No results found for your request!");
                logger.error(globalName+" requested yiff but No results found for your request!");
                return;
            }


            OffsetDateTime offsetCreateDate = OffsetDateTime.parse(post.created_at);
            ZonedDateTime parisDateTime = offsetCreateDate.atZoneSameInstant(ZoneId.of("Europe/Helsinki"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String finalDate = parisDateTime.format(formatter);

            //Checking for webm content
            if (post.file.ext.equalsIgnoreCase("webm")) {
                eb.addField("\uD83D\uDCF9 Full Video:", "*[Watch here]("+ post.file.url+")*",true);
                eb.setImage(post.sample.url);
            } else {
                eb.addField("\uD83D\uDDBC Original Image:", "*[View here]("+ post.file.url+")*",true);
                eb.setImage(post.file.url);
            }

            List<String> artists = new ArrayList<>();
            for (String artist : post.tags.artist) {
                artists.add("• ["+artist+"](https://e621.net/posts?tags="+artist+")");
            }

            eb.setTitle("❤️ Enjoy your yiff ❤️");
            eb.setDescription("\n**\uD83E\uDDD1\u200D\uD83C\uDFA8 Artists: **\n"+String.join("\n",artists)+"\n");
            eb.addField("\uD83C\uDF0D Search link: ", "[e621.net/posts](https://e621.net/posts?tags=" + String.join("+",tags).replace(":", "%3A").replace(" ", "%20") + ")",true);
            eb.addField("\uD83E\uDDD1 Uploader:","["+ post.uploader_id+"](https://e621.net/users/"+ post.uploader_id+")",true);
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

            reply(event, eb);

        } catch (Exception e) {
            replyError(event, "Failed to retrieve yiff from hell!");
            logger.error(globalName + "Requested yiff but failed to retrieve yiff!");
            e.printStackTrace();
        }
    }
}
