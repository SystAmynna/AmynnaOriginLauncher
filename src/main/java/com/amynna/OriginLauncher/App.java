package com.amynna.OriginLauncher;

import com.amynna.OriginLauncher.output.Logger;

public abstract class App {

    private static App instance;

    private static boolean isEnding;

    protected final void init() {
        // flag de fin
        isEnding = false;
        // vérifie qu'aucune instance n'existe déjà
        if (instance != null) Logger.fatal("Une instance de App existe déjà. Impossible d'en créer une nouvelle.");
        instance = this;
        // initialiser le repertoire racine de l'application
        FileManager.createDirectoriesIfNotExist(AppProperties.LAUNCHER_ROOT);
        // initialiser le logger
        Logger.init();
    }

    protected abstract void run();

    protected abstract void appEnd();

    public static void end() {
        // fermeture de l'enfant
        if (!isEnding) {
            isEnding = true;
            get().appEnd();
        }
        // Fermeture du Logger
        Logger.close();
        // fin
        System.exit(0);
    }

    public static App get() {
        if (instance == null) end();
        return instance;
    }




}
