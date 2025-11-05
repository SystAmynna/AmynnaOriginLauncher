package com.amynna.OriginLauncher.setup.modloaderSetup.forge;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Classe responsable de la gestion de Forge.
 */
public class ForgeManager {

    /**
     * Chemin vers l'exécutable Java.
     */
    private File installerFile;

    /**
     * Construit le ForgeManager.
     */
    public ForgeManager() {

    }


    public void setupForge() {

        Logger.log(Logger.PURPLE + "Installation de Forge...");

        downloadInstaller();

        runInstaller();

        JSONObject manifest = getManifest();
        assert manifest != null;


    }


    // --- MÉTHODES ---

    /**
     * Télécharge le JAR de l'installeur Forge.
     */
    private void downloadInstaller() {

        installerFile = AppProperties.TEMP_DIR.toPath().resolve("forge-installer-" + AppProperties.FORGE_ID + ".jar").toFile();

        try {
            FileManager.downloadFile(AppProperties.FORGE_INSTALLER_URL, installerFile.getAbsolutePath());
        } catch (Exception e) {
            Logger.error("Échec du téléchargement de l'installeur Forge : " + e.getMessage());
        }
    }

    /**
     * Exécute l'installeur de Forge en mode client sur le dossier .minecraft.
     */
    private void runInstaller() {
        if (installerFile == null || !installerFile.exists()) {
            Logger.error("Fichier installeur Forge non trouvé.");
            return;
        }

        ensureLauncherProfilesExist();

        // --- Construction de la commande ---
        // La commande minimale est : java -jar <installer.jar>
        // Le dossier .minecraft est souvent déduit si on utilise l'argument --dir

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
     * Récupère le manifeste de version Forge généré par l'installeur.
     * @return Le JSONObject du manifeste Forge.
     */
    private JSONObject getManifest() {

        if (!AppProperties.FORGE_MANIFEST.exists()) {
            Logger.error("Le manifeste Forge est introuvable : " + AppProperties.FORGE_MANIFEST.getAbsolutePath());
            return null;
        }

        // Retourner l'objet JSON
        return FileManager.openJsonFile(AppProperties.FORGE_MANIFEST);
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




}
