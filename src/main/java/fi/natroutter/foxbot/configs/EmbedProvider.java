package fi.natroutter.foxbot.configs;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.EmbedData;
import fi.natroutter.foxbot.configs.data.Placeholder;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.Handlers.*;
import lombok.Getter;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EmbedProvider {

    private static FoxLogger logger = FoxBot.getLogger();
    private DirectoryManager dirManager;

    private static ConcurrentHashMap<String, EmbedData> embeds = new ConcurrentHashMap<>();

    @Getter
    private boolean initialized = false;

    public EmbedProvider() {

        AtomicBoolean stage1 = new AtomicBoolean(false);
        AtomicBoolean stage2 = new AtomicBoolean(false);

        new MultiFileManager.Builder()
                .registerFiles(List.of(
                        "embeds/rules.json",
                        "embeds/daily_fox.json"
                ))
                .onErrorLog(e-> logger.error(e))
                .onInfoLog(e-> logger.info(e))
                .setLoading(false)
                .onInitialized((e)->{
                    stage1.set(true);
                })
                .setExportResource(true)
                .build();
        new FileManager.Builder("embeds/rules.json")
                .onErrorLog(e-> logger.error(e))
                .onInfoLog(e-> logger.info(e))
                .setLoading(false)
                .onInitialized((e)->{
                    stage1.set(true);
                })
                .setExportResource(true)
                .build();

        dirManager = new DirectoryManager.Builder()
                .setSubDirectory("embeds")
                .setAllowedExtensions(List.of("json"))
                .onInfoLog(e->logger.info(e))
                .onErrorLog(e->logger.error(e))
                .onInitialized(e->{
                    stage2.set(true);
                })
                .build();
        reload();
        initialized = (stage1.get() && stage2.get());
    }

    public void reload() {
        embeds.clear();
        dirManager.readAllFiles(file -> {
            if (file.success()) {
                String name = FileUtils.getBasename(file);
                loadData(name, file.content());
            }
        });
    }

    private void loadData(String name, String rawContent) {
        ParseData data = parseData(rawContent);
        if (data.embed() != null) {
            embeds.put(name, data.embed());
        } else {
            logger.error("Configuration error in (embeds/" + name + ") : " + data.message());
        }
    }

    public record ParseData(EmbedData embed,String message){};
    public ParseData parseData(String rawContent) {return parseData(rawContent,false);}
    public ParseData parseData(String rawContent, boolean base64) {
        if (base64) {
            rawContent = new String(Base64.getDecoder().decode(rawContent));
        }
        Gson gson = new Gson();
        try {
            EmbedData data = gson.fromJson(rawContent, EmbedData.class);
            return new ParseData(data, "success");
        } catch (JsonSyntaxException e) {
            return new ParseData(null, e.getCause().getMessage());
        }
    }

    public ConcurrentHashMap<String, EmbedData> get() {return embeds;}

    public EmbedData get(String name) {
        try {
            EmbedData data = embeds.get(name);
            if (data == null) return null;

            EmbedData.Embed embed = data.clone().getEmbed();
            if (embed == null) return null;

            return data;
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
        }
        return null;
    }

}
