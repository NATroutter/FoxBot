package fi.natroutter.foxbot.configs;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxlib.Handlers.FileManager;
import fi.natroutter.foxlib.Handlers.NATLogger;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

public class ConfigProvider {

    private Config config;

    @Getter
    private boolean initialized = false;

    private NATLogger logger = FoxBot.getLogger();

    public ConfigProvider() {
        FileManager fm = new FileManager.Builder("config.yaml")
                .setErrorLogger((error) -> logger.error(error))
                .setInfoLogger((Info)-> logger.info(Info))
                .build();

        if (fm.isInitialized()) {
            DumperOptions options = new DumperOptions();
            Representer representer = new Representer(options);
            representer.getPropertyUtils().setSkipMissingProperties(true);

            Yaml yaml = new Yaml(new Constructor(Config.class),representer);

            config = yaml.load(fm.get());
            initialized = true;
        }
    }

    public Config get() {return config;}

}
