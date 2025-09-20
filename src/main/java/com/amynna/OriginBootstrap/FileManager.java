package com.amynna.OriginBootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileManager {

    /**
     * Télécharge un fichier depuis une URL vers un chemin local
     *
     * @param urlString URL du fichier à télécharger
     * @param destinationPath Chemin local où enregistrer le fichier
     * @return Le fichier téléchargé ou null en cas d'erreur
     */
    public static File downloadFile(String urlString, String destinationPath) {
        try {
            // Créer le client HTTP
            HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            // Préparer la requête
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .GET()
                    .build();

            // Créer le fichier de destination
            File outputFile;
            if (destinationPath.endsWith(File.separator)) {
                // Si le chemin se termine par un séparateur, on utilise le nom du fichier de l'URL
                String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
                outputFile = new File(destinationPath + fileName);
            } else {
                outputFile = new File(destinationPath);
            }

            // Créer les répertoires parents si nécessaires
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            // Télécharger le fichier
            HttpResponse<Path> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofFile(outputFile.toPath()));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return outputFile;
            } else {
                System.out.println("Échec du téléchargement: code " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Erreur lors du téléchargement: " + e.getMessage());
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
            System.err.println("Erreur lors du téléchargement du fichier.");
            return null;
        }
        if (!KeyUtil.validateSignature(file)) {
            System.err.println("Le fichier téléchargé n'est pas signé avec une clé publique de confiance.");
            file.delete();
            return null;
        }
        return file;
    }



}
