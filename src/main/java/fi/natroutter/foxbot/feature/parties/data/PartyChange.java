package fi.natroutter.foxbot.feature.parties.data;

import lombok.Getter;

@Getter
public class PartyChange {

    private String key;
    private String oldValue;
    private String newValue;

    public PartyChange(String key, String oldValue, String newValue) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public PartyChange(String key, boolean oldValue, boolean newValue) {
        this.key = key;
        this.oldValue = oldValue ? "YES" : "NO";
        this.newValue = newValue ? "YES" : "NO";
    }

    public PartyChange(String key, boolean oldValue, boolean newValue, String trueName, String falseName) {
        this.key = key;
        this.oldValue = oldValue ? trueName : falseName;
        this.newValue = newValue ? trueName : falseName;
    }

    public PartyChange(String key, int oldValue, int newValue) {
        this.key = key;
        this.oldValue = String.valueOf(oldValue);
        this.newValue = String.valueOf(newValue);
    }

};
