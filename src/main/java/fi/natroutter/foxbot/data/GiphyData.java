package fi.natroutter.foxbot.data;

public class GiphyData {

    public data data;

    public class data {
        public String type;
        public String id;
        public String url;
        public Images images;
    }

    public class Images {
        public Original original;
    }

    public class Original {
        public String url;
    }

}
