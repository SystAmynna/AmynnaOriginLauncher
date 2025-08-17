package fr.amynna.OriginLauncher.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class Web {

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
                Printer.printError("Échec du téléchargement: code " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            Printer.printError("Erreur lors du téléchargement: " + e.getMessage());
            return null;
        }
    }

}
