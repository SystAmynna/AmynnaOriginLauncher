package com.amynna.OriginBootstrap;

import com.amynna.Tools.*;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Classe principale pour le lancement du launcher.
 * Elle vérifie la présence du launcher, sa version, et le télécharge si nécessaire.
 */
public final class Launch {

    /**
     * Nom du fichier du launcher.
     */
    private final String launcherName = AppProperties.APP_NAME + "_Launcher-" + AppProperties.APP_VERSION + ".jar";

    /**
     * Méthode principale pour vérifier et lancer le launcher.
     */
    public void process() {

        checkRootDir(); // Vérifier et créer le répertoire racine du launcher

        // Vérifier la présence du launcher et sa version
        if (!checkLauncher() || !checkVersion()) installLauncher();

        runLauncher(); // Lancer le launcher

    }

    /**
     * Méthode pour exécuter le launcher.
     * Utilise ProcessBuilder pour lancer le launcher avec les arguments nécessaires.
     */
    private void runLauncher() {
        List<String> cmd = new LinkedList<String>();

        cmd.add(AppProperties.foundJava());
        cmd.add("-jar");
        cmd.add(AppProperties.LAUNCHER_ROOT.getPath() + File.separator + launcherName);
        cmd.add("launch");


        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.inheritIO();
        try {
            Logger.log(Logger.PURPLE + Logger.BOLD + Logger.UNDERLINE + "Lancement du launcher...");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            Logger.log(Logger.PURPLE + Logger.BOLD + Logger.UNDERLINE + "Fin du launcher.");
            if (exitCode != 0) {
                Logger.fatal("Erreur lors du lancement du launcher.");
            }
        } catch (Exception e) {
            Logger.fatal("Erreur lors du lancement du launcher: " + e.getMessage());
        }
    }

    /**
     * Méthode pour vérifier et créer le répertoire racine du launcher.
     * Si le répertoire n'existe pas, il est créé.
     * Si un fichier avec le même nom existe, il est supprimé avant de créer le répertoire.
     */
    private void checkRootDir() {
        FileManager.createDirectoriesIfNotExist(AppProperties.LAUNCHER_ROOT.getPath());
        FileManager.createDirectoriesIfNotExist(AppProperties.TEMP_DIR.getPath());
    }

    /**
     * Méthode pour vérifier la présence du launcher et sa signature.
     * Si le launcher n'existe pas ou si la signature est invalide, il est téléchargé.
     * @return true si le launcher existe et est valide, false sinon.
     */
    private boolean checkLauncher() {
        // déclarer les fichiers
        File launcherFile = new File(AppProperties.LAUNCHER_ROOT.getPath() + File.separator + launcherName);
        File signatureFile = new File(KeyUtil.getSignaturePath(launcherName));
        SignedFile signedLauncher = new SignedFile(launcherFile, signatureFile);

        // vérifier l'existence des fichiers
        if (!signedLauncher.exists()) return false;

        return signedLauncher.valid();

    }

    /**
     * Méthode pour vérifier la version du launcher.
     * Télécharge le fichier de propriétés depuis le serveur, lit la dernière version,
     * et compare avec la version actuelle du launcher.
     * Si une nouvelle version est disponible, le launcher est téléchargé.
     * @return true si le launcher est à jour, false sinon.
     */
    private boolean checkVersion() {

        // Télécharger le fichier de propriétés depuis le serveur
        String onServerFileName = AppProperties.REPO_SERVER_URL + File.separator + AppProperties.APP_NAME + ".json";
        File propertiesFile = FileManager.downloadAndValidateFile(onServerFileName, AppProperties.LAUNCHER_ROOT.getPath() + onServerFileName);
        assert propertiesFile != null;

        // Lire le fichier de propriétés
        JSONObject properties = FileManager.openJsonFile(propertiesFile);
        assert properties != null;

        // Récupérer la dernière version du launcher
        String lastVersion = properties.getString("last_launcher_version");

        // Vérifier la version actuelle du launcher
        String currentVersion = "";
        // Exécuter le launcher avec l'argument "version" pour obtenir sa version actuelle
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", AppProperties.LAUNCHER_ROOT.getPath() + launcherName, "version");
        try {
            Process process = processBuilder.start();

            // obtenir le code de sortie du processus
            int exitCode = process.waitFor();
            if (exitCode != 0) Logger.fatal("Erreur lors de la vérification de la version du launcher.");

            // lire la sortie du processus
            currentVersion = new String(process.getInputStream().readAllBytes()).trim();
            if (!currentVersion.equals(lastVersion)) return  false;
            else Logger.log("Le launcher est à jour.");

        } catch (Exception e) {
            Logger.fatal("Erreur lors de la vérification de la version du launcher: " + e.getMessage());
        }

        return true;
    }

    private void installLauncher() {
        FileManager.deleteFileIfExists(new File(AppProperties.LAUNCHER_ROOT + launcherName));

        FileManager.downloadAndValidateFile(launcherName, AppProperties.LAUNCHER_ROOT.getPath() + File.separator + launcherName);
    }


}
