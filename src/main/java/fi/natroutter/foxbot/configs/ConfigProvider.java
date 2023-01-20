package fi.natroutter.foxbot.configs;

import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.provider.GenericType;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;

import java.nio.file.Paths;
import java.util.List;

public class ConfigProvider {

    ConfigurationProvider provider;

    public ConfigProvider() {
        this.provider = configurationProvider();
    }

    public String getString(ConfKeys key) {
        return provider.getProperty(key.getPath(), String.class);
    }

    public int getInteger(ConfKeys key) {
        return provider.getProperty(key.getPath(),  Integer.class);
    }

    public long getLong(ConfKeys key) {
        return provider.getProperty(key.getPath(),  Long.class);
    }

    public boolean getBool(ConfKeys key) {
        return provider.getProperty(key.getPath(),  Boolean.class);
    }

    public List<String> getStringList(ConfKeys key) {
        return  provider.getProperty("key", new GenericType<List<String>>() {});
    }

    private ConfigurationProvider configurationProvider() {
        ConfigFilesProvider provider = () -> List.of(Paths.get("config.yml"));
        ClasspathConfigurationSource source = new ClasspathConfigurationSource(provider);
        Environment environment = new ImmutableEnvironment("./");

        return new ConfigurationProviderBuilder()
                .withEnvironment(environment)
                .withConfigurationSource(source)
                .build();
    }

}
