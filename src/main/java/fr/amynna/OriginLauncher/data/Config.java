package fr.amynna.OriginLauncher.data;

import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Classe {@code Config} gère la configuration de l'application.
 * Elle permet de charger, sauvegarder et réinitialiser la configuration à partir d'un fichier JSON.
 */
public class Config {

    /**
     * Chemin vers le fichier de configuration.
     * Il est construit à partir du chemin racine défini dans la classe {@code Proprieties}.
     */
    public static final String CONFIG_PATH = Proprieties.ROOT_PATH + "/config.json";

    /**
     * Instance de la configuration sous forme de JSONObject.
     * Elle est initialisée à null et chargée à la demande.
     */
    private static JSONObject config = null;


    /**
     * Méthode statique pour charger la configuration depuis le fichier.
     * Si le fichier n'existe pas, elle réinitialise la configuration par défaut.
     */
    public static void loadConfig() {

        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            Printer.info("Configuration non trouvée, création d'une nouvelle configuration par défaut.");
            resetConfig();
            return;
        }

        config = FileManager.openJsonFile(CONFIG_PATH);
    }

    /**
     * Méthode statique pour sauvegarder la configuration dans le fichier.
     * Elle écrit le contenu de l'objet JSONObject dans le fichier JSON.
     */
    public static void saveConfig() {
        if (config == null) {
            Printer.error("Erreur lors du sauvegarde");
            return;
        }

        FileManager.saveJsonFile(config, CONFIG_PATH);
    }

    /**
     * Méthode statique pour réinitialiser la configuration à ses valeurs par défaut.
     * Elle crée un nouvel objet JSONObject avec les valeurs par défaut et le sauvegarde.
     */
    public static void resetConfig() {
        JSONObject config = new JSONObject();

        // TODO : Créer une vrai méthode de verification de la configuration
        config.put("mcInstalled", false);

        // RAM
        config.put("minRAM", 4);
        config.put("maxRAM", 6);



        Config.config = config;
        saveConfig();
    }

    /**
     * Méthodes pour mettre à jour les valeurs de la configuration.
     * Elles vérifient si la configuration est chargée, sinon elles la chargent,
     * puis mettent à jour la valeur associée à la clé spécifiée et sauvegardent la configuration.
     *
     * @param key La clé de la configuration à mettre à jour.
     * @param value La nouvelle valeur à associer à la clé.
     */
    public static void updateIntConfig(String key, int value) {
        if (config == null) {
            loadConfig();
        }
        config.put(key, value);
        saveConfig();
    }
    /**
     * Met à jour une valeur booléenne dans la configuration.
     * Si la configuration n'est pas chargée, elle est chargée d'abord.
     * Ensuite, la valeur associée à la clé spécifiée est mise à jour et la configuration est sauvegardée.
     *
     * @param key   La clé de la configuration à mettre à jour.
     * @param value La nouvelle valeur booléenne à associer à la clé.
     */
    public static void updateBooleanConfig(String key, boolean value) {
        if (config == null) {
            loadConfig();
        }
        config.put(key, value);
        saveConfig();
    }

    /**
     * Méthodes pour obtenir les valeurs de la configuration.
     * Elles vérifient si la configuration est chargée, sinon elles la chargent,
     * puis retournent la valeur associée à la clé spécifiée.
     *
     * @param key La clé de la configuration dont on veut obtenir la valeur.
     * @return La valeur associée à la clé, ou 0/false si la clé n'existe pas.
     */
    public static int getIntConfig(String key) {
        if (config == null) {
            loadConfig();
        }
        return config.optInt(key, 0); // Default to 0 if key not found
    }
    /**
     * Obtient une valeur booléenne de la configuration.
     * Si la configuration n'est pas chargée, elle est chargée d'abord.
     * Ensuite, la valeur associée à la clé spécifiée est retournée.
     *
     * @param key La clé de la configuration dont on veut obtenir la valeur booléenne.
     * @return La valeur booléenne associée à la clé, ou false si la clé n'existe pas.
     */
    public static boolean getBooleanConfig(String key) {
        if (config == null) {
            loadConfig();
        }
        return config.optBoolean(key, false); // Default to false if key not found
    }


}
