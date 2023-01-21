package fi.natroutter.foxbot.configs;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.utilities.NATLogger;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;

public class ConfigProvider {

    private Config config;

    private NATLogger logger = FoxBot.getLogger();

    @Getter
    private boolean initialized = false;

    public ConfigProvider() {
        FileHandler fh = new FileHandler("config.yaml");
        initialized = fh.isInitialized();
        if (fh.isInitialized()) {

            DumperOptions options = new DumperOptions();
            Representer representer = new Representer(options);
            representer.getPropertyUtils().setSkipMissingProperties(true);

            Yaml yaml = new Yaml(new Constructor(Config.class),representer);

            config = yaml.load(fh.load());
        }
    }
    public Config get() {
        return config;
    }

}
