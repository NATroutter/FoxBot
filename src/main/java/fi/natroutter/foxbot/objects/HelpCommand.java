package fi.natroutter.foxbot.objects;

import lombok.Getter;

public class HelpCommand {

    @Getter private String arg;
    @Getter private String description;
    @Getter private boolean raw;



    public HelpCommand(String arg, String description, boolean raw) {
        this.arg = arg;
        this.description = description;
        this.raw = raw;
    }

    public HelpCommand(String arg, String description) {
        this.arg = arg;
        this.description = description;
        this.raw = false;
    }
};

