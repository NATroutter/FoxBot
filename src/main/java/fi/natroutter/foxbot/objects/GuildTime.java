package fi.natroutter.foxbot.objects;

public record GuildTime(String joined, long days) {
    public GuildTime(){this("Unknown", 0L);}
}