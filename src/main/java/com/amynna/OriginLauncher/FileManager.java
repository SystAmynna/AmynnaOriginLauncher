package com.amynna.OriginLauncher;

import com.amynna.OriginLauncher.output.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileManager {

    /** Constructeur privé, n'est pas sensé être instancié. */
    private FileManager() {
        Logger.fatal("Impossible d'instancier FileManager. Cette classe est un utilitaire statique.");
    }

    /**
     * Crée les répertoires spécifiés dans le chemin s'ils n'existent pas.
     * Si un fichier existe déjà à cet emplacement, il est supprimé et remplacé par un répertoire.
     * @param directoryPath Le chemin du répertoire à créer
     */
    public static void createDirectoriesIfNotExist(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Logger.fatal("Échec de la création du répertoire : ", directoryPath);
            } else Logger.log(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD, "Répertoire créé : ", directoryPath);
        } else if (!dir.isDirectory()) {
            if (dir.delete() && dir.mkdirs()) {
                Logger.log(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD, "Fichier remplacé par un répertoire : ", directoryPath);
            } else {
                Logger.fatal("Échec de la création du répertoire (un fichier existe déjà) : ", directoryPath);
            }
        } else Logger.log(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.VERBOSE, "Repertoire ", directoryPath, " déjà existant, rien à faire");
    }
    /**
     * Crée les répertoires spécifiés dans le chemin s'ils n'existent pas.
     * Si un fichier existe déjà à cet emplacement, il est supprimé et remplacé par un répertoire.
     * @param directoryPath le chemin du répertoire à créer
     */
    public static void createDirectoriesIfNotExist(Path directoryPath) {
        createDirectoriesIfNotExist(directoryPath.toString());
    }
    /**
     * Crée les répertoires spécifiés dans le chemin s'ils n'existent pas.
     * Si un fichier existe déjà à cet emplacement, il est supprimé et remplacé par un répertoire.
     * @param directory le répertoire à créer
     */
    public static void createDirectoriesIfNotExist(File directory) {
        createDirectoriesIfNotExist(directory.getPath());
    }



    /**
     * Supprime un fichier ou un répertoire (et son contenu) s'il existe.
     *
     * @param file Fichier ou répertoire à supprimer
     */
    public static void deleteFileIfExists(File file) {
        if (!file.exists()) {
            Logger.log(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.VERBOSE, "Fichier ou répertoire non trouvé (rien à supprimer) : " + file.getPath());
            return;
        }

        // Si c'est un répertoire, supprimer son contenu récursivement
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteFileIfExists(f);
                }
            }
        }

        // Supprimer le fichier ou le répertoire maintenant vide
        if (!file.delete()) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD, "Impossible de supprimer : " + file.getPath());
        } else Logger.log(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD, "Supprimé : " + file.getPath());
    }

    /**
     * Recherche un fichier par nom dans un répertoire et ses sous-répertoires.
     *
     * @param directory Répertoire de départ pour la recherche
     * @param fileName  Nom du fichier à rechercher
     * @return Le fichier trouvé, ou null si non trouvé
     */
    public static File searchFileInDirectory(File directory, String fileName) {
        if (!directory.isDirectory()) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD, "Le chemin spécifié n'est pas un répertoire : " + directory.getPath());
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD,"Impossible de lister les fichiers dans le répertoire : " + directory.getPath());
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = searchFileInDirectory(file, fileName);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().equals(fileName)) {
                return file;
            }
        }

        return null; // Fichier non trouvé
    }

    /**
     * Ouvre un fichier JSON et le charge dans un objet JSONObject.
     *
     * @param file Fichier JSON à ouvrir
     * @return Un objet JSONObject contenant les données du fichier, ou null en cas d'erreur
     */
    public static JSONObject openJsonFile(File file) {

        if (!file.exists()) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD,"Le fichier JSON n'existe pas : " + file.getPath());
            return null;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return new JSONObject(content);
        } catch (IOException e) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors de la lecture du fichier JSON : " + e.getMessage());
            return null;
        }
    }

    /**
     * Sauvegarde un objet JSONObject dans un fichier JSON.
     *
     * @param jsonObject Objet JSONObject à sauvegarder
     * @param file       Fichier de destination
     */
    public static File saveJsontoFile(JSONObject jsonObject, File file) {
        try {
            Files.writeString(file.toPath(), jsonObject.toString(4), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return file;
        } catch (IOException e) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
            return null;
        }
    }


}
