package com.amynna.OriginBootstrap;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileManager {

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
            System.out.println("\uD83D\uDCE5 Fichier téléchargé dans : " + downloadedFile.getAbsolutePath());

            return downloadedFile;

        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement du fichier : " + e.getMessage());
            return null;
        }


    }

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

    public static File downloadAndValidateFile(String urlString, String destinationPath) {
        File file = downloadFile(urlString, destinationPath);
        if (file == null || !file.exists()) {
            System.err.println("Erreur lors du téléchargement du fichier...");
            return null;
        }
        if (!KeyUtil.validateSignature(file)) {
            System.err.println("Le fichier téléchargé n'est pas signé avec une clé publique de confiance.");
            file.delete();
            return null;
        }
        return file;
    }


    /**
     * Extrait les données originales d'un fichier signé et les enregistre dans un nouveau fichier.
     * @param signedFile Le fichier signé contenant la signature et les données
     * @param outputPath Le chemin où sauvegarder le fichier extrait (si null, crée un fichier temporaire)
     * @return Le fichier contenant les données originales, ou null en cas d'erreur
     */
    public static File extractOriginalDataToFile(File signedFile, String outputPath) {
        try {
            // Lire le contenu du fichier signé
            byte[] signedFileBytes = Files.readAllBytes(signedFile.toPath());

            // Vérifier que le fichier est assez grand pour contenir une signature
            if (signedFileBytes.length <= KeyUtil.RSA_SIGNATURE_SIZE) {
                System.err.println("Fichier trop petit pour contenir une signature valide");
                return null;
            }

            // Extraire les données originales
            byte[] originalData = Arrays.copyOfRange(signedFileBytes, KeyUtil.RSA_SIGNATURE_SIZE, signedFileBytes.length);

            // Créer le fichier de sortie
            File outputFile;
            if (outputPath != null) {
                outputFile = new File(outputPath);
                if (outputFile.getParentFile() != null && !outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
            } else {
                // Créer un fichier temporaire avec le même nom mais sans signature
                String originalName = signedFile.getName();
                String extension = "";
                int dotIndex = originalName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = originalName.substring(dotIndex);
                    originalName = originalName.substring(0, dotIndex);
                }
                outputFile = File.createTempFile(originalName + "-unsigned", extension);
            }

            // Écrire les données dans le fichier
            Files.write(outputFile.toPath(), originalData);

            return outputFile;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction des données : " + e.getMessage());
            return null;
        }
    }


}
