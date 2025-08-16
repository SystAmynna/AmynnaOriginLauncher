package fr.amynna.OriginLauncher.tools;

import fr.amynna.OriginLauncher.Proprieties;

import java.io.File;

/**
 * Classe {@code Setup} qui gère la configuration initiale de l'application.
 * Elle est responsable de la création des répertoires nécessaires et du téléchargement
 * des éléments nécessaires.
 */
public final class Setup {

    /**
     * Méthode principale qui initialise l'application.
     */
    private static void createDirectories() {
        // verifier si le répertoire racine existe, sinon le créer
        File rootDir = new File(Proprieties.ROOT_PATH);
        File launcherDir = new File(Proprieties.LAUNCHER_PATH);
        File minecraftDir = new File(Proprieties.MC_PATH);
        try {
            if (!rootDir.exists()) {
                if (rootDir.mkdirs() && launcherDir.mkdirs() && minecraftDir.mkdirs()) {
                    Printer.printInfo("Répertoire racine créé : " + Proprieties.ROOT_PATH);
                    Printer.printInfo("Répertoire launcher créé : " + Proprieties.ROOT_PATH);
                    Printer.printInfo("Répertoire minecraft créé : " + Proprieties.ROOT_PATH);
                    return;
                } else {
                    Printer.fatalError("Impossible de créer le répertoire racine : " + Proprieties.ROOT_PATH);
                }
            }

            if (!launcherDir.exists()) {
                if (launcherDir.mkdirs()) {
                    Printer.printInfo("Répertoire launcher créé : " + Proprieties.LAUNCHER_PATH);
                } else {
                    Printer.fatalError("Impossible de créer le répertoire launcher : " + Proprieties.LAUNCHER_PATH);
                }
            }

            if (!minecraftDir.exists()) {
                if (minecraftDir.mkdirs()) {
                    Printer.printInfo("Répertoire minecraft créé : " + Proprieties.MC_PATH);
                } else {
                    Printer.fatalError("Impossible de créer le répertoire minecraft : " + Proprieties.MC_PATH);
                }
            } else {
                Printer.printInfo("Répertoire minecraft déjà existant : " + Proprieties.MC_PATH);
            }

        } catch (SecurityException se) {
            Printer.fatalError("Erreur de sécurité lors de la création du répertoire racine : " + se.getMessage());
        }
    }

    private static void downloadMojangManifest() {
        //
    }

}
