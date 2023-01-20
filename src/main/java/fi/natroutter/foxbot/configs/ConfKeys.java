package fi.natroutter.foxbot.configs;

import lombok.Getter;

public enum ConfKeys {

    TOKEN("Token"),
    OWNER("OwnerID"),

    THEME_RED("ThemeColor.Red"),
    THEME_GREEN("ThemeColor.Green"),
    THEME_BLUE("ThemeColor.Blue"),

    DB_DATABASE("MongoDB.Database"),
    DB_USER("MongoDB.Username"),
    DB_PASS("MongoDB.Password"),
    DB_HOST("MongoDB.Host"),
    DB_PORT("MongoDB.Port");


    @Getter
    private String path;
    ConfKeys(String path) {
        this.path = path;
    }
    ConfKeys() {
        this.path = this.name().toLowerCase();
    }

}
