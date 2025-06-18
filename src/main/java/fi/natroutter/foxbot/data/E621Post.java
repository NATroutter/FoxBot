package fi.natroutter.foxbot.data;

import java.util.ArrayList;

public class E621Post {

    public int id;
    public String created_at;
    public String updated_at;
    public File file;
    public Score score;
    public String rating;
    public int fav_count;
    public Tags tags;
    public Sample sample;
    public int uploader_id;


    public class Sample{
        public boolean has;
        public int height;
        public int width;
        public String url;
        public Alternates alternates;
    }

    public class Alternates{
        public Original original;
    }

    public class Original{
        public String type;
        public int height;
        public int width;
        public ArrayList<String> urls;
    }

    public class File {
        public int width;
        public int height;
        public String ext;
        public int size;
        public String md5;
        public String url;
    }

    public class Tags{
        public ArrayList<String> artist;
    }

    public class Score {
        public int up;
        public int down;
        public int total;
    }

}
