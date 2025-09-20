package com.amynna.OriginBootstrap.tasks;

import com.amynna.OriginBootstrap.App;
import com.amynna.OriginBootstrap.FileManager;
import com.amynna.OriginBootstrap.KeyUtil;

import java.io.File;
import java.util.Map;

public final class Launch {

    private final App app;
    private final String launcherName = "launcher.jar";

    private boolean downloaded = false;

    public Launch(App app) {
        this.app = app;
    }

    public void process() {

        /*
        TODO:
        - lancer l'archive avec la commande
         */

        checkRootDir(); // Vérifier et créer le répertoire racine du launcher

        checkLauncher(); // Vérifier, télécharger le launcher si nécessaire, et vérifier la signature

        if (!downloaded) checkVersion(); // Vérifier la version si le launcher n'a pas été téléchargé

        runLauncher(); // Lancer le launcher

    }

    private void runLauncher() {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", app.LAUNCHER_ROOT + launcherName, "launch", app.SERVER_URL);
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


    private void checkRootDir() {
        File rootDir = new File(app.LAUNCHER_ROOT);
        boolean success = true;
        if (rootDir.exists()) {
            success = rootDir.mkdirs();
        } else if (!rootDir.isDirectory()) {
            success = rootDir.delete();
            if (success) success = rootDir.mkdirs();
        }
        if (!success) {
            System.err.println("Erreur lors de la création du répertoire racine du launcher.");
            System.exit(-1);
        }
    }

    private void checkLauncher() {
        File launcherFile = new File(app.LAUNCHER_ROOT + launcherName);
        if (!launcherFile.exists() || !validateSignature(launcherFile)) downloadLauncher();
    }

    private void checkVersion() {

        String onServerFileName = app.APP_NAME + ".properties";

        File propertiesFile = FileManager.downloadAndValidateFile(app.SERVER_URL + onServerFileName, app.LAUNCHER_ROOT + onServerFileName);

        Map<String, String> Properties = FileManager.readKeyValueTextFile(propertiesFile);

        String lastVersion = Properties.get("LauncherVersion");

        ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", app.LAUNCHER_ROOT + launcherName, "version");
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


    private void downloadLauncher() {
        File launcherFile = new File(app.LAUNCHER_ROOT + launcherName);
        if (launcherFile.exists()) launcherFile.delete();
        FileManager.downloadAndValidateFile(app.LAUNCHER_ROOT + launcherName, app.LAUNCHER_ROOT + launcherName );
        downloaded = true;
    }

    private boolean validateSignature(File launcherFile) {
        return KeyUtil.validateSignature(launcherFile);
    }



}
