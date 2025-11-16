package fi.natroutter.foxbot.configs;

import com.google.gson.Gson;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.AIRequest;
import fi.natroutter.foxlib.FoxLib;
import fi.natroutter.foxlib.files.DirectoryManager;
import fi.natroutter.foxlib.files.MultiFileManager;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIRequestProvider {

    private static Gson gson = new Gson();
    private static FoxLogger logger = FoxBot.getLogger();
    private DirectoryManager dirManager;

    private static ConcurrentHashMap<String, AIRequest> models = new ConcurrentHashMap<>();

    private static final String directory = "ai-requests";
    private static final List<String> registeredModels = List.of(
            directory+"/grammar.json"
    );

    @Getter
    private boolean initialized = false;

    public AIRequestProvider() {

        AtomicBoolean stage1 = new AtomicBoolean(false);
        AtomicBoolean stage2 = new AtomicBoolean(false);

        new MultiFileManager.Builder()
                .registerFiles(registeredModels)
                .onErrorLog(e-> logger.error(e))
                .onInfoLog(e-> logger.info(e))
                .setLoading(false)
                .onInitialized((e)->{
                    stage1.set(true);
                })
                .setExportResource(true)
                .build();

        dirManager = new DirectoryManager.Builder()
                .setSubDirectory(directory)
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
        models.clear();
        dirManager.readAllFiles(file -> {
            if (file.success()) {
                String name = FoxLib.getBasename(file);
                loadData(name, file.content());
            }
        });
    }

    private void loadData(String name, String rawContent) {
        AIRequest model = gson.fromJson(rawContent, AIRequest.class);
        if (model != null) {
            models.put(name, model);
        } else {
            logger.error("Configuration error in ("+directory+"/" + name + ")");
        }
    }

    public ConcurrentHashMap<String, AIRequest> get() {return models;}

    public AIRequest get(String name) {
        return models.getOrDefault(name, null);
    }

}
