package com.amynna.OriginLauncher.setup.vanillaSetup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * Classe responsable de la gestion du manifeste Minecraft.
 */
public class McManifestHandler {

    // ----[ ATTRIBUTS ]----

    /** Manifeste Minecraft au format JSON. */
    private final JSONObject mcManifest;

    /** Gestionnaire des bibliothèques Minecraft. */
    private final McLibManager mcLibManager;
    /** Gestionnaire des assets Minecraft. */
    private final McAssetManager mcAssetManager;
    /** Gestionnaire du démarrage de Minecraft. */
    private final McStartManager mcStartManager;

    /** Classe représentant le client Minecraft. */
    private record McClient(String url, String sha1, int size, File file) {
        private McClient {}
    }
    /** Client Minecraft. */
    private McClient mcClient;


    /** Constructeur de la classe McManifestHandler.
     *
     * @param manifest Le manifeste Minecraft au format JSON.
     */
    protected McManifestHandler(JSONObject manifest) {
        // Initialisation du manifest
        this.mcManifest = manifest;

        // Vérification du manifest
        assert mcManifest != null;
        assert mcManifest.getString("id").equals(AppProperties.MINECRAFT_VERSION);

        // Récupération du client Minecraft
        this.mcClient = parseMcClient();

        // Récupération des bibliothèques
        JSONArray libs = mcManifest.getJSONArray("libraries");
        assert libs != null;
        // Initialisation du gestionnaire des bibliothèques
        this.mcLibManager = new McLibManager(libs);

        // Récupération des assets
        String assetsVal = mcManifest.getString("assets");
        assert  assetsVal != null && !assetsVal.isEmpty();
        JSONObject assetIndex = mcManifest.getJSONObject("assetIndex");
        assert assetIndex != null;
        // Initialisation du gestionnaire des assets
        this.mcAssetManager = new McAssetManager(assetsVal, assetIndex);

        // Récupération des informations de démarrage
        JSONObject args = mcManifest.getJSONObject("arguments");
        String mainClass = mcManifest.getString("mainClass");
        JSONObject logging = mcManifest.getJSONObject("logging");
        String classpath = mcLibManager.generateClasspath(mcClient.file);
        String assetIndexName = mcAssetManager.assets;
        String versionType = mcManifest.getString("type");
        // Initialisation du gestionnaire de démarrage
        this.mcStartManager = new McStartManager(args, mainClass, logging, classpath, assetIndexName, versionType);

    }

    // ----[ GESTION DU CLIENT ]----

    /** Récupère les informations du client Minecraft depuis le manifeste.
     *
     * @return Le client Minecraft.
     */
    private McClient parseMcClient() {
        // Récupération des informations du client Minecraft
        JSONObject downloads = mcManifest.getJSONObject("downloads");
        assert downloads != null;
        JSONObject mcClient = downloads.getJSONObject("client");
        assert mcClient != null;

        // Extraction des données
        String url = mcClient.getString("url");
        String sha1 = mcClient.getString("sha1");
        int size = mcClient.getInt("size");
        File file = AppProperties.MINECRAFT_CLIENT;

        // Création et retour de l'objet McClient
        return new McClient(url, sha1, size, file);
    }

    /**
     * Télécharge le client Minecraft s'il n'est pas déjà présent.
     */
    private void downloadMcClient() {
        // Vérification légère avant le téléchargement
        if (lightCheckMcClient()) return;

        // Téléchargement du client Minecraft
        FileManager.downloadFileAndVerifySha1(
                mcClient.url,
                mcClient.file.getPath(),
                mcClient.sha1
        );
    }

    /**
     * Vérifie l'intégrité du client Minecraft.
     * Si le fichier est manquant ou corrompu, il est retéléchargé.
     */
    private void checkMcClient() {

        Logger.logc("Vérification du Client... ");

        // Vérification légère
        lightCheckMcClient();

        // Vérification du SHA1
        String fileSha1 = FileManager.calculSHA1(mcClient.file);
        assert fileSha1 != null;
        if (fileSha1.equals(mcClient.sha1)) {
            Logger.log(Logger.GREEN + "[OK]");
            return;
        }

        Logger.log(Logger.RED + "[CORROMPU]");

        // Suppression du fichier corrompu
        FileManager.deleteFileIfExists(mcClient.file);
        // Retéléchargement du client Minecraft
        downloadMcClient();
    }

    /**
     * Effectue une vérification légère du client Minecraft.
     *
     * @return true si le fichier existe et sa taille correspond, false sinon.
     */
    private boolean lightCheckMcClient() {
        return mcClient != null
                && mcClient.file != null
                && mcClient.file.exists()
                && mcClient.file.length() == mcClient.size;
    }

    // ----[ MÉTHODES PUBLIQUES ]----

    /**
     * Configure les fichiers Minecraft nécessaires.
     */
    public void setupMinecraftFiles() {
        // Téléchargement du client Minecraft
        Logger.log(Logger.GREEN + "Vérification du client Minecraft...");
        downloadMcClient();

        // Téléchargement des bibliothèques Minecraft
        Logger.log(Logger.GREEN + "Vérification des bibliothèques Minecraft...");
        mcLibManager.downloadAllLibraries();

        // Téléchargement des assets Minecraft
        Logger.log(Logger.GREEN + "Vérification des assets Minecraft...");
        mcAssetManager.downloadAllAssets();
    }

    /**
     * Vérifie l'intégrité des fichiers Minecraft.
     */
    public void checkMinecraftFiles() {
        // Vérification du client Minecraft
        Logger.log(Logger.GREEN + "Vérification avancée du client Minecraft...");
        checkMcClient();

        // Vérification des bibliothèques Minecraft
        Logger.log(Logger.GREEN + "Vérification avancée des bibliothèques Minecraft...");
        mcLibManager.checkAllLibraries();

        // Vérification des assets Minecraft
        Logger.log(Logger.GREEN + "Vérification avancée des assets Minecraft...");
        mcAssetManager.checkAllAssets();
    }

    /**
     * Démarre Minecraft.
     */
    public void startMinecraft() {
        // Extraction des natives
        mcLibManager.extractNatives();

        // Démarrage de Minecraft
        mcStartManager.startMinecraft();
    }



}
