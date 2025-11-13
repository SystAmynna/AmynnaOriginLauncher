package com.amynna.OriginLauncher;

import com.amynna.OriginLauncher.setup.GameSetup;
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
     * Indicateur pour quitter l'application.
     */
    private boolean shouldExit = false;

    /**
     * Instance de l'authentification.
     */
    private final Auth auth;

    /**
     * Instance du gestionnaire de l'installation du jeu.
     */
    private final GameSetup gameSetup;

    /**
     * Constructeur privé pour empêcher l'instanciation externe.
     */
    private App () {
        // Initialisation du singleton
        auth = new Auth();
        gameSetup = new GameSetup();
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
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_VERSION_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_LIB_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_ASSETS_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_ASSETS_OBJECTS_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_ASSETS_INDEX_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_NATIVES_DIR.getAbsolutePath());
    }

    // −−−-[ PROCESSUS PRINCIPAL ]----

    /**
     * Démarre le processus principal de l'application.
     */
    public void launch() {
        while (!shouldExit) {
            int choice = Asker.askMenu();

            switch (choice) {
                case 0 -> startGame();
                case 1 -> verifyInstallation();
                case 2 -> authentifie();
                case 3 -> showSettings();
                case 4 -> uninstallGame();
                default -> System.exit(0);
            }
        }
    }


    /**
     * Lance le processus d'authentification.
     */
    private void authentifie() {
        Logger.log(Logger.PURPLE + "[CALL] Authentification de l'utilisateur...");
        auth.authentifie();
    }

    /**
     * Lance le jeu.
     */
    private void startGame() {
        Logger.log(Logger.PURPLE + "[CALL] Démarrage du jeu...");
        if (!auth.isAuthenticated()) auth.authentifie();
        gameSetup.setup();
        gameSetup.startGame();
        shouldExit = true;
    }

    /**
     * Affiche les paramètres de l'application.
     */
    private void showSettings() {
        Logger.log(Logger.PURPLE + "[CALL] Settings...");

    }

    /**
     * Vérifie l'installation du jeu.
     */
    private void verifyInstallation() {
        Logger.log(Logger.PURPLE + "[CALL] Vérification de l'installation du jeu...");
        //gameSetup.repairGame();
    }

    private void uninstallGame() {
        Logger.log(Logger.PURPLE + "[CALL] Désinstallation du jeu...");
        //gameSetup.uninstallGame();
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
