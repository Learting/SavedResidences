package cc.globalserver.SavedResidences;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class LanguageProperty {
    private YamlConfiguration config;

    public void loadConfiguration(File langFile) {
        config = YamlConfiguration.loadConfiguration(langFile);
    }

    public String get(String key){
        return config.getString(key);
    }
}
