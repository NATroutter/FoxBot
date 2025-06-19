package fi.natroutter.foxbot.feature.parties.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
public class SettingChange {

    private String key;
    private String oldValue;
    private String newValue;

    public SettingChange(String key, String oldValue, String newValue) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public SettingChange(String key, boolean oldValue, boolean newValue) {
        this.key = key;
        this.oldValue = oldValue ? "YES" : "NO";
        this.newValue = newValue ? "YES" : "NO";
    }

    public SettingChange(String key, boolean oldValue, boolean newValue, String trueName, String falseName) {
        this.key = key;
        this.oldValue = oldValue ? trueName : falseName;
        this.newValue = newValue ? trueName : falseName;
    }

    public SettingChange(String key, int oldValue, int newValue) {
        this.key = key;
        this.oldValue = String.valueOf(oldValue);
        this.newValue = String.valueOf(newValue);
    }

};
