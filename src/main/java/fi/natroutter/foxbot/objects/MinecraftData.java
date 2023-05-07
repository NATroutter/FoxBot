package fi.natroutter.foxbot.objects;

public record MinecraftData(String name, String id) {
    public MinecraftData(){
        this("Unknown", "Unknown");
    }
}