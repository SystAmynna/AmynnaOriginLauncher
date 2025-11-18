package com.amynna.OriginLauncher;

import com.amynna.OriginLauncher.setup.GameSetup;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Asker;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;

/**
 * la classe principale {@code App} du Launcher. Elle gère le cycle de vie de l'application,
 * et orchestre que tous les modules principaux.
 */
public final class App {

    // −−−-[ PROPRIÉTÉS ]----

    /**
     * Instance unique de l'application (singleton).
     */
    private static App instance;

    /**
     * Indicateur pour quitter l'application. Si vrai, le processus principal s'arrête.
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

        if (!AppProperties.pingServer()) {
            Logger.fatal("Impossible de contacter le serveur.", 0);
        }
        Logger.log(Logger.BLUE + Logger.BOLD + "Connexion au serveur réussie.");

        // Configuration des répertoires
        setupDirs();
        // Initialisation du singleton
        auth = new Auth();
        gameSetup = new GameSetup();

    }

    // −−−-[ GETTERS ]----

    /**
     * Récupère l'instance unique de l'application.
     * @return {@code App} L'instance de l'application.
     */
    public static App get() {
        if (instance == null) {
            instance = new App();
        }
        return instance;
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
     * Créer les répertoires nécessaires au fonctionnement de l'application,
     * et nettoie le répertoire temporaire.
     */
    private void setupDirs() {
        FileManager.createDirectoriesIfNotExist(AppProperties.LAUNCHER_ROOT.getAbsolutePath());
        FileManager.deleteFileIfExists(AppProperties.TEMP_DIR); // Nettoie le répertoire
        FileManager.createDirectoriesIfNotExist(AppProperties.TEMP_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.SIGNATURE_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_VERSION_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_LIB_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_ASSETS_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_ASSETS_OBJECTS_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_ASSETS_INDEX_DIR.getAbsolutePath());
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_NATIVES_DIR.getAbsolutePath());

        FileManager.createDirectoriesIfNotExist(AppProperties.MODS_DIR.getAbsolutePath());
    }

    // −−−-[ PROCESSUS PRINCIPAL ]----

    /**
     * Démarre le processus principal de l'application, accède aux actions de l'utilisateur via le menu.
     */
    public void launch() {

        // TODO : Remplacer avec une interface graphique

        while (!shouldExit) {
            int choice = Asker.askMenu();

            switch (choice) {
                case 0 -> startGame();
                case 1 -> verifyInstallation();
                case 2 -> authentifie();
                case 3 -> showSettings();
                case 4 -> AdminIdentificator.checkAdmin();
                default -> System.exit(0);
            }
        }
    }

    // −−−-[ ACTIONS ]----

    /**
     * Lance l'action principale d'authentification.
     */
    private void authentifie() {
        Logger.log(Logger.PURPLE + "[CALL] Authentification de l'utilisateur...");
        auth.authentifie();
    }

    /**
     * Lance l'action principale de démarrage du jeu.
     */
    private void startGame() {
        Logger.log(Logger.PURPLE + "[CALL] Démarrage du jeu...");
        // S'assure que l'utilisateur est authentifié avant de lancer le jeu
        if (!auth.isAuthenticated()) auth.authentifie();
        // Prépare et démarre le jeu
        gameSetup.setup();
        // Lance le jeu
        gameSetup.startGame();
        // Permet l'arrêt du launcher après le lancement du jeu
        shouldExit = true;
    }

    /**
     * Lance l'action principale des paramètres.
     */
    private void showSettings() {
        Logger.log(Logger.PURPLE + "[CALL] Settings...");

        // TODO : Implémenter les paramètres

    }

    /**
     * Lance l'action principale de vérification de l'installation du jeu.
     */
    private void verifyInstallation() {
        Logger.log(Logger.PURPLE + "[CALL] Vérification de l'installation du jeu...");
        gameSetup.checkInstallation();
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
