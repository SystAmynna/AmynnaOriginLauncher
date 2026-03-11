package com.amynna.OriginLauncher.download;

import com.amynna.OriginLauncher.AppProperties;
import com.amynna.OriginLauncher.FileManager;
import com.amynna.OriginLauncher.output.Logger;
import com.amynna.OriginLauncher.security.SignedFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class DownloadHandler {

    /**
     * Télécharge un fichier depuis une URL et le sauvegarde dans le chemin spécifié.
     * Si le chemin de destination est un répertoire, le nom du fichier est déterminé automatiquement.
     * @param url URL du fichier à télécharger
     * @param destinationPath Chemin de destination (fichier ou répertoire)
     * @return Le fichier téléchargé, ou null en cas d'erreur
     */
    public static File downloadFile(String url, String destinationPath) {

        int lastSlashIndex = url.lastIndexOf('/');
        String baseUrl = url.substring(0, lastSlashIndex + 1);
        String fileNameURL = url.substring(lastSlashIndex + 1);

        // Encoder uniquement le nom du fichier
        String encodedFileName = URLEncoder.encode(fileNameURL, StandardCharsets.UTF_8);
        String encodedUrl = baseUrl + encodedFileName;

        try {

            URL website = new URL(encodedUrl);

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
            Logger.log(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD, Logger.BLUE + "\uD83D\uDCE5 Fichier téléchargé : " + encodedUrl + " ➔ " + downloadedFile.getAbsolutePath());

            return downloadedFile;

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.FILES, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors du téléchargement du fichier " + encodedUrl + " : " + e.getMessage());
            return null;
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
    public static SignedFile downloadAndValidateFile(String onServerPath, String destinationPath) {

        // Chemin du fichier et de sa signature sur le serveur
        String fileOnServerPath = AppProperties.REPO_SERVER_URL + onServerPath;

        // Télécharger le fichier principal
        File file = downloadFile(fileOnServerPath, destinationPath);
        if (file == null || !file.exists()) {
            Logger.error(Logger.LOG_CANAL.DOWNLOAD, Logger.VERBOSE_LEVEL.STANDARD, "Erreur lors du téléchargement du fichier...");
            return null;
        }

        // Télécharger le fichier de signature
        File signatureFile = downloadSignatureFile(onServerPath);

        SignedFile signedFile = new SignedFile(file, signatureFile);

        if (!signedFile.valid()) {
            Logger.error(Logger.LOG_CANAL.DOWNLOAD, Logger.VERBOSE_LEVEL.STANDARD,"Le fichier téléchargé n'est pas signé avec une clé publique de confiance.");
            signedFile.delete();
            return null;
        }
        return signedFile;
    }

    /**
     * Télécharge le fichier de signature depuis le serveur en fonction de l'URL du fichier principal.
     * @param originalOnServerPath URL du fichier principal sur le serveur.
     * @return Le fichier de signature téléchargé.
     */
    public static File downloadSignatureFile(String originalOnServerPath) {

        // Chemin du fichier de signature sur le serveur
        String signOnServerPath = AppProperties.SIGNATURE_LOCATION_ON_SERVER + originalOnServerPath + AppProperties.SIGNATURE_FILE_EXTENSION;

        // Emplacement local du fichier de signature
        String localSignPath = AppProperties.SIGNATURE_DIR.getPath() + File.separator + originalOnServerPath + AppProperties.SIGNATURE_FILE_EXTENSION;

        // Télécharger le fichier de signature
        File signatureFile = downloadFile(signOnServerPath, localSignPath);
        if (signatureFile == null || !signatureFile.exists()) {
            Logger.error(Logger.LOG_CANAL.DOWNLOAD, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors du téléchargement du fichier de signature...");
        }

        // Télécharger le fichier de signature
        return signatureFile;
    }

    /**
     * Calcule et vérifie le hachage SHA-1 d'un fichier.
     *
     * @return Le hachage SHA-1 calculé ou null en cas d'erreur
     */
    public static String calculSHA(File file, String shaType) {

        try {
            MessageDigest digest = MessageDigest.getInstance(shaType);

            try (InputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                Logger.error(Logger.LOG_CANAL.DOWNLOAD, Logger.VERBOSE_LEVEL.STANDARD,"Erreur de lecture du fichier pour le calcul " + shaType + " : " + e.getMessage());
                return null;
            }

            // Convertir le hachage en hexadécimal
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Logger.fatal("Algorithme " + shaType + " non trouvé : " + e.getMessage());
            return null;
        }
    }

    /**
     * Télécharge un fichier depuis une URL et vérifie son hachage SHA-1.
     *
     * @param url URL du fichier à télécharger
     * @param destinationPath Chemin local où enregistrer le fichier
     * @param expectedSha Hachage SHA-1 attendu pour le fichier
     * @return true si le téléchargement et la vérification réussissent, false sinon
     */
    public static File downloadFileAndVerifySha(String url, String destinationPath, String expectedSha, String shaType) {
        File downloadedFile = downloadFile(url, destinationPath);
        if (downloadedFile == null) {
            return null; // Échec du téléchargement
        }

        try {
            String calculatedSha1 = calculSHA(downloadedFile, shaType);
            if (calculatedSha1 != null && calculatedSha1.equals(expectedSha)) {
                return downloadedFile; // Vérification réussie
            }
        } catch (SecurityException e) {
            Logger.error(Logger.LOG_CANAL.DOWNLOAD, Logger.VERBOSE_LEVEL.STANDARD, "Erreur de sécurité lors de la vérification " + shaType + " : " + e.getMessage());
        }

        FileManager.deleteFileIfExists(downloadedFile);
        return null;

    }



}
