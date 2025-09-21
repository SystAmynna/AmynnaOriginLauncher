package com.amynna.Tools;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestionnaire de fichiers pour le téléchargement et la lecture de fichiers.
 */
public final class FileManager {

    private FileManager() {
        // Constructeur privé pour empêcher l'instanciation
        Logger.fatal("FileManager ne peut pas être instancié.");
    }

    /**
     * Télécharge un fichier depuis une URL et le sauvegarde dans le chemin spécifié.
     * Si le chemin de destination est un répertoire, le nom du fichier est déterminé automatiquement.
     * @param url URL du fichier à télécharger
     * @param destinationPath Chemin de destination (fichier ou répertoire)
     * @return Le fichier téléchargé, ou null en cas d'erreur
     */
    public static File downloadFile(String url, String destinationPath) {

        try {

            URL website = new URL(url);

            // Ouvrir la connexion HTTP
            HttpURLConnection connection = (HttpURLConnection) website.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Échec du téléchargement, code HTTP : " + connection.getResponseCode());
            }

            // Déterminer le nom du fichier
            String fileName = null;

            // 1. Vérifier l'en-tête "Content-Disposition"
            String disposition = connection.getHeaderField("Content-Disposition");
            if (disposition != null && disposition.contains("filename=")) {
                fileName = disposition.split("filename=")[1].replace("\"", "").trim();
            }

            // 2. Sinon, prendre le dernier segment de l’URL
            if (fileName == null || fileName.isEmpty()) {
                String urlPath = website.getPath();
                fileName = urlPath.substring(urlPath.lastIndexOf("/") + 1);
                if (fileName.isEmpty()) {
                    fileName = "downloaded_file"; // fallback
                }
            }

            // Construire le vrai chemin de destination
            Path destination = Paths.get(destinationPath);
            if (Files.isDirectory(destination) || destinationPath.endsWith("/") || destinationPath.endsWith("\\")) {
                destination = destination.resolve(fileName);
            }

            // Créer les dossiers si besoin
            Files.createDirectories(destination.getParent());

            // Télécharger le fichier
            try (InputStream in = connection.getInputStream();
                 OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            File downloadedFile = destination.toFile();
            System.out.println("\uD83D\uDCE5 Fichier téléchargé : " + url + " -> " + downloadedFile.getAbsolutePath());

            return downloadedFile;

        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement du fichier : " + e.getMessage());
            return null;
        }


    }

    /**
     * Lit un fichier texte contenant des paires clé:valeur et les retourne dans une Map.
     * Les lignes vides ou sans ':' sont ignorées.
     * @param file Le fichier texte à lire
     * @return Une Map contenant les paires clé:valeur
     */
    public static Map<String, String> readKeyValueTextFile(File file) {
        Map<String, String> map = new HashMap<>();
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !line.contains(":")) continue;
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();
                map.put(key, value);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
        return map;
    }

    /**
     * Crée les répertoires spécifiés dans le chemin s'ils n'existent pas.
     * Si un fichier existe déjà à cet emplacement, il est supprimé et remplacé par un répertoire.
     * @param directoryPath Le chemin du répertoire à créer
     */
    public static void createDirectoriesIfNotExist(String directoryPath) {
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                Logger.log("Répertoire créé : " + directoryPath);
            } else {
                Logger.fatal("Échec de la création du répertoire : " + directoryPath);
            }
        } else if (!dir.isDirectory()) {
            if (dir.delete() && dir.mkdirs()) {
                Logger.log("Fichier remplacé par un répertoire : " + directoryPath);
            } else {
                Logger.fatal("Échec de la création du répertoire (un fichier existe déjà) : " + directoryPath);
            }
        }
    }

    /**
     * Télécharge un fichier depuis une URL, le sauvegarde dans le chemin spécifié,
     * puis valide sa signature avec une clé publique de confiance.
     * Si la validation échoue, le fichier est supprimé.
     * @param onServerPath URL du fichier à télécharger
     * @param destinationPath Chemin de destination (fichier ou répertoire)
     * @return Le fichier téléchargé et validé, ou null en cas d'erreur ou de validation échouée
     */
    public static File downloadAndValidateFile(String onServerPath, String destinationPath) {

        onServerPath = AppProperties.REPO_SERVER_URL + File.separator + onServerPath;

        File file = downloadFile(onServerPath, destinationPath);

        if (file == null || !file.exists()) {
            Logger.error("Erreur lors du téléchargement du fichier...");
            return null;
        }

        String onServerSignPath = AppProperties.SIGNATURE_LOCATION_ON_SERVER + file.getName() + AppProperties.SIGNATURE_FILE_EXTENSION;
        String localSignPath = AppProperties.SIGNATURE_DIR.getPath() + File.separator + file.getName() + AppProperties.SIGNATURE_FILE_EXTENSION;

        File signatureFile = downloadFile(onServerSignPath, localSignPath);

        if (signatureFile == null || !signatureFile.exists()) {
            Logger.error("Erreur lors du téléchargement du fichier de signature...");
        }

        SignedFile signedFile = new SignedFile(file, signatureFile);

        if (!KeyUtil.validateSignature(signedFile)) {
            Logger.error("Le fichier téléchargé n'est pas signé avec une clé publique de confiance.");
            file.delete();
            signatureFile.delete();
            return null;
        }
        return file;
    }


}
