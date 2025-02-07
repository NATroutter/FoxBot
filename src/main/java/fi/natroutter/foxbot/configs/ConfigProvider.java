package fi.natroutter.foxbot.configs;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxlib.files.FileManager;
import fi.natroutter.foxlib.logger.FoxLogger;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

public class ConfigProvider {

    private Config config;

    @Getter
    private boolean initialized = false;

    private FoxLogger logger = FoxBot.getLogger();

    public ConfigProvider() {
        new FileManager.Builder("config.yaml")
                .onErrorLog((error) -> logger.error(error))
                .onInfoLog((Info)-> logger.info(Info))
                .onInitialized(file -> {
                    if (file.success()) {
                        DumperOptions options = new DumperOptions();
                        Representer representer = new Representer(options);
                        representer.getPropertyUtils().setSkipMissingProperties(true);

                        Constructor constructor = new Constructor(Config.class, new LoaderOptions());
                        Yaml yaml = new Yaml(constructor, representer);

                        config = yaml.loadAs(file.content(), Config.class);
                    }
                    initialized = file.success();
                })
                .build();
    }

    public Config get() {return config;}

}
