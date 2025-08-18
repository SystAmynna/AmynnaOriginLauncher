package fr.amynna.OriginLauncher.work;

import fr.amynna.OriginLauncher.data.Proprieties;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Classe {@code Setup} qui gère la configuration initiale de l'application.
 * Elle est responsable de la création des répertoires nécessaires et du téléchargement
 * des éléments nécessaires.
 */
public final class Setup {

    private static String JDK_PATH = Proprieties.ROOT_PATH + "/java";

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



    /**
     * Télécharge et installe Java 17 adapté au système courant
     * @param installDir répertoire cible où installer Java
     */
    /*
    public static Path downloadAndExtractJava17() {
        String osKey = Proprieties.getOsKey().toLowerCase();
        String archiveUrl;

        // Choix du bon binaire selon l’OS
        switch (osKey) {
            case "windows":
                archiveUrl = "https://api.adoptium.net/v1/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse";
                break;
            case "linux":
                archiveUrl = "https://api.adoptium.net/v1/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse";
                break;
            case "mac":
                archiveUrl = "https://api.adoptium.net/v1/binary/latest/17/ga/mac/x64/jdk/hotspot/normal/eclipse";
                break;
            default:
                Printer.fatalError("Système d'exploitation non supporté pour le téléchargement de Java 17 : " + osKey);
                return null;
        }

        // Téléchargement de l’archive
        Path archivePath = Paths.get(JDK_PATH);
        try {
            Files.createDirectories(archivePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        FileManager.downloadFile(archiveUrl, archivePath.toString() + "/jdk.zip");

        // Décompression (ZIP uniquement ici, pour tar.gz il faudrait Apache Commons Compress)
        if (archivePath.toString().endsWith(".zip")) {
            unzip(archivePath, installDir);
        } else {
            throw new UnsupportedOperationException("Décompression tar.gz non implémentée (Linux/Mac)");
        }

        // Retourne le dossier d’installation
        return installDir;
    }


     */

}
