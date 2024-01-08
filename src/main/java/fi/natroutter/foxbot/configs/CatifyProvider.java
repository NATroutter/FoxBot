package fi.natroutter.foxbot.configs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxlib.Handlers.FileManager;
import fi.natroutter.foxlib.Handlers.FoxLogger;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

public class CatifyProvider {

    @Getter
    private boolean initialized = false;

    private FoxLogger logger = FoxBot.getLogger();

    @Getter
    private ConcurrentHashMap<String, String> replacements = new ConcurrentHashMap<>();

    public CatifyProvider() {
        FileManager fm = new FileManager.Builder("catify.json")
                .setErrorLogger((error) -> logger.error(error))
                .setInfoLogger((Info)-> logger.info(Info))
                .build();

        if (fm.isInitialized()) {

            JsonObject json = JsonParser.parseString(fm.get()).getAsJsonObject();

            json.keySet().forEach(key -> {
                replacements.put(key, json.get(key).getAsString());
            });

            initialized = true;
        }
    }

}
