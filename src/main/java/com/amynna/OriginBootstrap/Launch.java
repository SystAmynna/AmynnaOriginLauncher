package com.amynna.OriginBootstrap;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.KeyUtil;

import java.io.File;
import java.util.Map;

/**
 * Classe principale pour le lancement du launcher.
 * Elle vérifie la présence du launcher, sa version, et le télécharge si nécessaire.
 */
public final class Launch {

    /**
     * Instance de l'application contenant les configurations.
     */
    private final App app;
    /**
     * Nom du fichier du launcher.
     */
    private final String launcherName = "launcher.jar";

    /**
     * Indicateur si le launcher a été téléchargé.
     */
    private boolean downloaded = false;

    /**
     * Constructeur de la classe Launch.
     *
     * @param app Instance de l'application contenant les configurations.
     */
    public Launch(App app) {
        this.app = app;
    }

    /**
     * Méthode principale pour vérifier et lancer le launcher.
     */
    public void process() {

        checkRootDir(); // Vérifier et créer le répertoire racine du launcher

        checkLauncher(); // Vérifier, télécharger le launcher si nécessaire, et vérifier la signature

        if (!downloaded) checkVersion(); // Vérifier la version si le launcher n'a pas été téléchargé

        runLauncher(); // Lancer le launcher

    }

    /**
     * Méthode pour exécuter le launcher.
     * Utilise ProcessBuilder pour lancer le launcher avec les arguments nécessaires.
     */
    private void runLauncher() {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", AppProperties.LAUNCHER_ROOT.getPath() + launcherName, "launch");
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Erreur lors du lancement du launcher.");
                System.exit(-1);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du lancement du launcher: " + e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Méthode pour vérifier et créer le répertoire racine du launcher.
     * Si le répertoire n'existe pas, il est créé.
     * Si un fichier avec le même nom existe, il est supprimé avant de créer le répertoire.
     */
    private void checkRootDir() {
        File rootDir = new File(AppProperties.LAUNCHER_ROOT.getPath());
        boolean success = true;
        if (!rootDir.exists()) {
            success = rootDir.mkdirs();
        } else if (!rootDir.isDirectory()) {
            success = rootDir.delete();
            if (success) success = rootDir.mkdirs();
        }

        File tempDir = AppProperties.TEMP_DIR;
        if (success && !tempDir.exists()) success = tempDir.mkdirs();
        else if (success && tempDir.isDirectory()) {
            success = tempDir.delete();
            if (success) success = tempDir.mkdirs();
        }

        if (!success) {
            System.err.println("Erreur lors de la création du répertoire racine du launcher.");
            System.exit(-1);
        }
    }

    /**
     * Méthode pour vérifier la présence du launcher et sa signature.
     * Si le launcher n'existe pas ou si la signature est invalide, il est téléchargé.
     */
    private void checkLauncher() {
        File launcherFile = new File(AppProperties.LAUNCHER_ROOT.getPath() + launcherName);
        if (!launcherFile.exists() || !KeyUtil.validateSignature(launcherFile)) downloadLauncher();
    }

    /**
     * Méthode pour vérifier la version du launcher.
     * Télécharge le fichier de propriétés depuis le serveur, lit la dernière version,
     * et compare avec la version actuelle du launcher.
     * Si une nouvelle version est disponible, le launcher est téléchargé.
     */
    private void checkVersion() {

        String onServerFileName = AppProperties.LAUNCHER_ROOT + ".properties";

        File propertiesFile = FileManager.downloadAndValidateFile(AppProperties.LAUNCHER_ROOT.getPath() + onServerFileName, AppProperties.LAUNCHER_ROOT.getPath() + onServerFileName);

        Map<String, String> Properties = FileManager.readKeyValueTextFile(propertiesFile);

        String lastVersion = Properties.get("LauncherVersion");

        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", AppProperties.LAUNCHER_ROOT.getPath() + launcherName, "version");
        try {
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Erreur lors de la vérification de la version du launcher.");
                System.exit(-1);
            }
            String currentVersion = new String(process.getInputStream().readAllBytes()).trim();
            if (!currentVersion.equals(lastVersion)) {
                System.out.println("Une nouvelle version du launcher est disponible. Téléchargement...");
                downloadLauncher();
            } else {
                System.out.println("Le launcher est à jour.");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de la version du launcher: " + e.getMessage());
            System.exit(-1);
        }


    }

    /**
     * Méthode pour télécharger le launcher depuis le serveur.
     * Si un fichier du launcher existe déjà, il est supprimé avant le téléchargement.
     * Après le téléchargement, l'indicateur 'downloaded' est mis à true.
     */
    private void downloadLauncher() {
        File launcherFile = new File(AppProperties.LAUNCHER_ROOT.getPath() + launcherName);
        if (launcherFile.exists()) {
            try { launcherFile.delete(); } catch (Exception e) {
                System.err.println("Erreur lors de la suppression de l'ancien launcher: " + e.getMessage());
                System.exit(-1);
            }
        }
        FileManager.downloadAndValidateFile(AppProperties.LAUNCHER_ROOT.getPath() + launcherName, AppProperties.LAUNCHER_ROOT.getPath() + launcherName );
        downloaded = true;
    }



}
