package fi.natroutter.foxbot.configs;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.utilities.NATLogger;
import lombok.Getter;

import java.io.*;

public class FileHandler {


    private String fileName;
    private String subFolder;
    private String FileContent;

    private File file;
    private File fileFolder;

    @Getter
    private boolean initialized = false;

    public FileHandler(String fileName) {
        this.fileName = fileName;
        this.subFolder = "";
        initialize();
    }

    public FileHandler(String subFolder, String fileName) {
    	this.fileName = fileName;
    	this.subFolder = subFolder;
        initialize();
    }

    private void initialize() {
        file = new File(System.getProperty("user.dir") + "/" + (subFolder.length() > 0 ? subFolder + "/" : "") + fileName);
        fileFolder = new File(file.getParent());

        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }
        if (!file.exists()) {
            if (!exportResource(file, fileName)) {
                return;
            }
        }
        reload();
    }

    public void reload() {
        FileContent = readFile(file);
        if (FileContent != null) {
            info(fileName + " Loaded!");
            initialized = true;
        }
    }

    public String load() { return FileContent; }

    public void save(String data) {
    	writeFile(file, data);
    }

    private boolean exportResource(File file, String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try(InputStream stream = classLoader.getResourceAsStream(resourceName); OutputStream resStreamOut = new FileOutputStream(file);) {
            if(stream == null) {
                error("Failed to export resource : " + resourceName);
                return false;
            }

            int readBytes;
            byte[] buffer = new byte[4096];
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private String readFile(File file) {
        try (FileReader fr = new FileReader(file); BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line).append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            error("Failed to read file!");
            e.printStackTrace();
        }
        return null;
    }

    private boolean writeFile(File file, String Content) {
        try(FileWriter fw = new FileWriter(file);BufferedWriter bw = new BufferedWriter(fw);) {
            if (!file.exists()) {
                file.createNewFile();
            }
            bw.write(Content);
            return true;
        } catch (Exception e) {
            error("Failed to write file!");
            e.printStackTrace();
        }
        return false;
    }

    private void error(String message) {
        System.out.println("[FileManager] [ERROR] " + message);
    }
    private void info(String message) {
        System.out.println("[FileManager] [INFO] " + message);
    }

}
