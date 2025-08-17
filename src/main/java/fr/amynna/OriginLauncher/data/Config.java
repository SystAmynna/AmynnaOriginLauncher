package fr.amynna.OriginLauncher.data;

import fr.amynna.OriginLauncher.tools.Printer;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {

    public static final String CONFIG_PATH = Proprieties.ROOT_PATH + "/config.json";

    private static JSONObject config = null;


    public static void loadConfig() {

        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            resetConfig();
            return;
        }


        // TODO méthodes IO JSON
        try {
            String content = Files.readString(Path.of(CONFIG_PATH));
            config = new JSONObject(content);
            Printer.printInfo("Configuration chargée avec succès.");
        } catch (IOException e) {
            Printer.printError("Erreur lors du chargement de la configuration: " + e.getMessage());
            resetConfig();
        }
    }

    public static void saveConfig() {
        if (config == null) {
            Printer.printError("Erreur lors du sauvegarde");
            return;
        }

        try {
            // Save the config to the file
            Files.write(Paths.get(CONFIG_PATH), config.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }

        Printer.printInfo("Config sauvegardée avec succès.");
    }

    public static void resetConfig() {
        JSONObject config = new JSONObject();

        config.put("mcInstalled", false);

        // RAM
        config.put("minRAM", 4);
        config.put("maxRAM", 6);



        Config.config = config;
        Printer.printInfo("Configuration réinitialisée avec succès.");
        saveConfig();

    }

    public static void updateIntConfig(String key, int value) {
        if (config == null) {
            loadConfig();
        }
        config.put(key, value);
        saveConfig();
    }
    public static void updateBooleanConfig(String key, boolean value) {
        if (config == null) {
            loadConfig();
        }
        config.put(key, value);
        saveConfig();
    }


    public static int getIntConfig(String key) {
        if (config == null) {
            loadConfig();
        }
        return config.optInt(key, 0); // Default to 0 if key not found
    }
    public static boolean getBooleanConfig(String key) {
        if (config == null) {
            loadConfig();
        }
        return config.optBoolean(key, false); // Default to false if key not found
    }


}
