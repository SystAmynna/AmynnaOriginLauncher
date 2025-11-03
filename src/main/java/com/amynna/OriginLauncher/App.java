package com.amynna.OriginLauncher;

import com.amynna.OriginLauncher.setup.GameSetupManager;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Asker;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;

/**
 * la classe principale {@code App} du Launcher.
 */
public final class App {

    // −−−-[ PROPRIÉTÉS ]----

    /**
     * Instance unique de l'application (singleton).
     */
    private final static App INSTANCE = new App();

    /**
     * Instance de l'authentification.
     */
    private final Auth auth;

    /**
     * Instance du gestionnaire de l'installation du jeu.
     */
    private final GameSetupManager gameSetupManager;

    /**
     * Constructeur privé pour empêcher l'instanciation externe.
     */
    private App () {
        // Initialisation du singleton
        auth = new Auth();
        gameSetupManager = new GameSetupManager();
        // Configuration des répertoires
        setupDirs();
    }

    // −−−-[ GETTERS ]----

    /**
     * Récupère l'instance unique de l'application.
     * @return {@code App} L'instance de l'application.
     */
    public static App get() {
        return INSTANCE;
    }

    /**
     * Récupère l'instance d'authentification.
     * @return {@code Auth} L'instance d'authentification.
     */
    public Auth getAuth() {
        return auth;
    }

    // -−−−-[ MÉTHODES ]----

    /**
     * Configure les répertoires nécessaires au fonctionnement de l'application.
     */
    private void setupDirs() {
        FileManager.createDirectoriesIfNotExist(AppProperties.LAUNCHER_ROOT.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.TEMP_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.SIGNATURE_DIR.getAbsolutePath());
    }

    // −−−-[ PROCESSUS PRINCIPAL ]----

    /**
     * Démarre le processus principal de l'application.
     */
    public void launch() {

        int choice = Asker.askMenu();

        switch (choice) {
            case 0 -> installGame();
            case 1 -> startGame();
            case 2 -> authentifie();
            case 3 -> showSettings();
            case 4 -> verifyInstallation();
            default -> System.exit(0);
        }

    }


    /**
     * Lance le processus d'authentification.
     */
    private void authentifie() {
        auth.authentifie();
    }

    /**
     * Lance le processus d'installation du jeu.
     */
    private void installGame() {
        gameSetupManager.setupGame();
    }

    /**
     * Lance le jeu.
     */
    private void startGame() {

    }

    /**
     * Affiche les paramètres de l'application.
     */
    private void showSettings() {

    }

    /**
     * Vérifie l'installation du jeu.
     */
    private void verifyInstallation() {
        gameSetupManager.checkGameSetup();
    }

    // −−−-[ MAIN ]----

    /**
     * Point d'entrée principal de l'application.
     * @param args Arguments de la ligne de commande.
     */
    public static void main(String[] args) {

        // Si le programme est lancé sans arguments (ex: double-clic sur le JAR)
        if (args.length == 0) {
            Asker.askInfo("Vous n'êtes pas sensé démarrer ce programme directement.\n" +
                    "Veuillez utiliser le launcher fourni. (" +
                    AppProperties.APP_NAME + "-X.X.X.jar)\n" +
                    "Voir le serveur Discord pour plus d'informations.\n\n" +
                    "Application par " + AppProperties.APP_AUTHOR +
                    " | Version " + AppProperties.APP_VERSION);
            return;
        }

        // Affiche la version si demandé
        if (args[0].equals("version")) Logger.version();
        // Lance le launcher si demandé
        else if (args[0].equals("launch")) App.get().launch();

    }

}
