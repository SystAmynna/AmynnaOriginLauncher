package fr.amynna.OriginLauncher.work;

import fr.amynna.OriginLauncher.data.Proprieties;
import fr.amynna.OriginLauncher.tools.Printer;

import java.io.File;

/**
 * Classe {@code Setup} qui gère la configuration initiale de l'application.
 * Elle est responsable de la création des répertoires nécessaires et du téléchargement
 * des éléments nécessaires.
 */
public final class Setup {

    /**
     * Fil d'exécution pour la mise en place des fichiers et des répertoires.
     */
    public static void process() {
        // Création des répertoires nécessaires
        createDirectories();

        // ...
    }


    /**
     * Méthode principale qui initialise l'application.
     */
    private static void createDirectories() {
        // repertoires de l'application
        File rootDir = new File(Proprieties.ROOT_PATH);
        File minecraftDir = new File(Proprieties.MC_PATH);
        // Création des répertoires nécessaires
        try {
            if (!rootDir.exists()) {
                // Si le répertoire racine n'existe pas, on le crée ainsi que les sous-répertoires
                if (rootDir.mkdirs()  && minecraftDir.mkdirs()) {
                    Printer.info("Répertoire racine créé : " + Proprieties.ROOT_PATH);
                    Printer.info("Répertoire minecraft créé : " + Proprieties.ROOT_PATH);
                    return;
                } else {
                    Printer.fatalError("Impossible de créer le répertoire racine : " + Proprieties.ROOT_PATH);
                }
            }

            // Si le répertoire racine existe, on vérifie les sous-répertoires


            if (!minecraftDir.exists()) {
                if (minecraftDir.mkdirs()) {
                    Printer.info("Répertoire minecraft créé : " + Proprieties.MC_PATH);
                } else {
                    Printer.fatalError("Impossible de créer le répertoire minecraft : " + Proprieties.MC_PATH);
                }
            }

        } catch (SecurityException se) {
            Printer.fatalError("Erreur de sécurité lors de la création du répertoire racine : " + se.getMessage());
        }
    }



}
