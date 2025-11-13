package com.amynna.OriginLauncher.setup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * La classe {@code GameSetup} gère l'installation et la configuration du jeu.
 */
public class GameSetup {

    /** Gestionnaire des bibliothèques Minecraft. */
    private final LibManager libManager;
    /** Gestionnaire des assets Minecraft. */
    private final AssetManager assetManager;
    /** Gestionnaire du client Minecraft. */
    private final ClientManager clientManager;
    /** Gestionnaire du lancement du jeu. */
    private final LaunchHandler launchHandler;

    /** Manifeste de la version spécifique de Minecraft au format JSON. */
    private JSONObject mcVersionManifest;

    /** Manifeste de la version spécifique de Forge au format JSON. */
    private JSONObject forgeVersionManifest;

    /** Constructeur */
    public GameSetup() {

        // ----[ VANILLA ]----

        // Récupére le manifeste de la version spécifique de Minecraft
        installVersionManifest();

        // Traitement des bibliothèques 1
        final JSONArray libraries = mcVersionManifest.optJSONArray("libraries");
        assert libraries != null;
        libManager = new LibManager();
        libManager.updateLibList(libraries); // ajoute les libs à la liste

        // Récupération des assets
        String assetsVal = mcVersionManifest.getString("assets");
        assert  assetsVal != null && !assetsVal.isEmpty();
        JSONObject assetIndex = mcVersionManifest.getJSONObject("assetIndex");
        assert assetIndex != null;
        // Initialisation du gestionnaire des assets
        this.assetManager = new AssetManager(assetsVal, assetIndex);

        // Initialisation du gestionnaire du client
        final JSONObject downloads = mcVersionManifest.getJSONObject("downloads");
        this.clientManager = new ClientManager(downloads);

        // ----[ LAUNCHER ]----

        // Initialisation du gestionnaire de lancement
        this.launchHandler = new LaunchHandler();

        // Récupération de la classe principale
        final String mainClass = mcVersionManifest.getString("mainClass");
        launchHandler.setMainClass(mainClass);

        // Récupération du type de version
        final String type = mcVersionManifest.getString("type");
        launchHandler.setVersionType(type);

        // Récupération du nom de l'index des assets
        final String assetIndexName = assetIndex.getString("id");
        launchHandler.setAssetIndexName(assetIndexName);

    }


    /**
     * Effectue l'installation complète du jeu Minecraft avec Forge.
     */
    public void setup() {

        // ----[ INSTALLATION VANILLA ]----

        // Installation des bibliothèques Minecraft
        Logger.log(Logger.GREEN + Logger.BOLD + "Installation des bibliothèques Minecraft...");
        libManager.downloadAllLibraries();

        // Installation des assets Minecraft
        Logger.log(Logger.GREEN + Logger.BOLD + "Installation des assets Minecraft...");
        assetManager.downloadAllAssets();

        // Installation du client Minecraft
        Logger.log(Logger.GREEN + Logger.BOLD + "Installation du client Minecraft...");
        clientManager.downloadMcClient();

        // ----[ INSTALLATION FORGE ]----

        // Installation de Forge
        Logger.log(Logger.GREEN + Logger.BOLD + "Installation de Forge...");
        installForge();
        forgeSetup();

        // Mise à jour des bibliothèques Minecraft avec celles de Forge
        Logger.log(Logger.GREEN + Logger.BOLD + "Mise à jour des bibliothèques Minecraft pour Forge...");
        libManager.downloadAllLibraries();

        Logger.log(Logger.GREEN + Logger.BOLD + "Décompression des bibliothèques natives...");
        libManager.extractNatives();

    }

    /**
     * Vérifie l'installation complète du jeu Minecraft avec Forge.
     */
    public void checkInstallation() {

        libManager.checkAllLibraries();
        assetManager.checkAllAssets();
        clientManager.checkMcClient();

    }

    /**
     * Démarre le jeu Minecraft avec Forge.
     */
    public void startGame() {

        final JSONObject mcArgs = mcVersionManifest.getJSONObject("arguments");
        assert mcArgs != null;

        final JSONObject forgeArgs = forgeVersionManifest.getJSONObject("arguments");
        assert forgeArgs != null;

        launchHandler.setClasspath(libManager.generateClasspath());
        launchHandler.loadManifest(mcArgs);
        launchHandler.loadManifest(forgeArgs);

        launchHandler.start();

    }

    // ----[ MÉTHODES PRIVÉES ]----

    /**
     * Télécharge et installe le manifeste de la version spécifique de Minecraft.
     */
    private void installVersionManifest() {

        // Téléchargement du fichier manifest.json de Mojang
        File mojangManifestFile = FileManager.downloadFile(AppProperties.MOJANG_MANIFEST_URL, AppProperties.MOJANG_MANIFEST.getPath());
        assert mojangManifestFile != null;

        // Lecture et analyse du fichier manifest.json de Mojang
        JSONObject mojangManifest = FileManager.openJsonFile(mojangManifestFile);
        assert mojangManifest != null;

        // Recherche de la version spécifique de Minecraft
        String versionUrl = null; // URL de la version spécifique de Minecraft
        String versionSha1 = null; // SHA1 de la version spécifique de Minecraft
        JSONArray versionsArray = mojangManifest.getJSONArray("versions"); // Tableau des versions disponibles
        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            if (versionObj.getString("id").equals(AppProperties.MINECRAFT_VERSION)) {
                versionUrl = versionObj.getString("url");
                versionSha1 = versionObj.getString("sha1");
                break;
            }
        }
        assert versionUrl != null && versionSha1 != null;

        // Téléchargement du fichier version.json de la version spécifique de Minecraft
        File versionFile = FileManager.downloadFileAndVerifySha1(versionUrl, AppProperties.VERSION_MANIFEST.getPath(), versionSha1);
        assert versionFile != null;

        // Lecture et analyse du fichier version.json vers JSON
        mcVersionManifest = FileManager.openJsonFile(versionFile);
        assert mcVersionManifest != null;
    }

    /**
     * Télécharge le JAR de l'installeur Forge.
     */
    private File downloadInstaller() {
        File forgeInstallerFile = AppProperties.TEMP_DIR.toPath().resolve("forge-installer-" + AppProperties.FORGE_ID + ".jar").toFile();
        FileManager.downloadFile(AppProperties.FORGE_INSTALLER_URL, forgeInstallerFile.getAbsolutePath());
        return forgeInstallerFile;
    }

    /**
     * Exécute l'installeur de Forge en mode client sur le dossier .minecraft.
     */
    private void installForge() {

        if (checkForgeInstallation()) {
            Logger.log(Logger.GREEN + "Forge est déjà installé.");
            return;
        }

        File installerFile = downloadInstaller();

        if (!installerFile.exists()) {
            Logger.error("Fichier installeur Forge non trouvé.");
            return;
        }

        ensureLauncherProfilesExist();

        // --- Construction de la commande ---

        List<String> command = new LinkedList<>();

        command.add(AppProperties.foundJava()); // 1. Exécutable Java
        command.add("-jar");             // 2. Argument -jar

        command.add(installerFile.getAbsolutePath()); // 3. Chemin de l'installeur

        command.add("--installClient"); // 4. Installer le client

        command.add("--installClient");        // 5. Spécifier le dossier d'installation
        command.add(AppProperties.MINECRAFT_DIR.getPath()); // Le dossier où il doit créer le profil

        // --- Exécution ---
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO(); // Affiche la sortie de l'installeur dans la console du launcher

        try {
            Logger.log(Logger.GREEN + "Lancement de l'installeur Forge...");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();


            if (exitCode == 0) {
                Logger.log(Logger.GREEN + "Installeur Forge terminé avec succès (code 0).");
            } else {
                Logger.error("L'installeur Forge a échoué (code " + exitCode + ").");
            }
        } catch (IOException | InterruptedException e) {
            Logger.error("Erreur lors de l'exécution de l'installeur Forge : " + e.getMessage());
        }

    }

    /**
     * Crée un fichier launcher_profiles.json minimal pour satisfaire l'installeur Forge.
     */
    private void ensureLauncherProfilesExist() {
        Path profilesPath = AppProperties.MINECRAFT_DIR.toPath().resolve("launcher_profiles.json");

        if (Files.exists(profilesPath)) {
            // Le fichier existe déjà, pas besoin de le recréer
            return;
        }

        String minimalProfilesJson =
                "{\n" +
                        "  \"profiles\": {\n" +
                        "    \"" + AppProperties.APP_NAME + "LauncherProfile\": {\n" +
                        "      \"name\": \"" + AppProperties.APP_NAME + "LauncherProfile\",\n" +
                        "      \"type\": \"custom\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"settings\": {\n" +
                        "    \"clientId\": \"client_id_placeholder\"\n" +
                        "  },\n" +
                        "  \"version\": 2\n" +
                        "}";

        try {

            Files.writeString(profilesPath, minimalProfilesJson);
            Logger.log("Création de launcher_profiles.json pour satisfaire Forge.");
        } catch (IOException e) {
            Logger.error("Impossible de créer launcher_profiles.json : " + e.getMessage());
        }
    }

    /**
     * Vérifie si Forge est déjà installé en vérifiant la présence du manifeste Forge.
     * @return true si Forge est installé, false sinon
     */
    private boolean checkForgeInstallation() {
        return AppProperties.FORGE_MANIFEST.exists();
    }

    private void forgeSetup() {

        forgeVersionManifest = FileManager.openJsonFile(AppProperties.FORGE_MANIFEST);
        assert forgeVersionManifest != null;

        final JSONArray forgeLibraries = forgeVersionManifest.optJSONArray("libraries");
        assert forgeLibraries != null;

        libManager.updateLibList(forgeLibraries);

        // Récupération de la classe principale de Forge
        final String forgeMainClass = forgeVersionManifest.getString("mainClass");
        launchHandler.setMainClass(forgeMainClass);

        // Récupération du type de version de Forge
        final String forgeType = forgeVersionManifest.getString("type");
        launchHandler.setVersionType(forgeType);

    }






}
