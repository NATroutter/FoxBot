package fi.natroutter.foxbot.utilities;

public class NATLogger {

    public void info(String msg) {
        log("[INFO] " + msg);
    }

    public void error(String msg) {
        log("[ERROR] " + msg);
    }

    public void warn(String msg) {
        log("[WARN] " + msg);
    }

    public void log(String msg) {
        System.out.println(msg);
    }

}
