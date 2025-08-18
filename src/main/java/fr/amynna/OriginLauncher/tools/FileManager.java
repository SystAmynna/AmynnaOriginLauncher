package fr.amynna.OriginLauncher.tools;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Classe {@code FileManager} fournit des méthodes pour gérer les fichiers,
 * y compris la sauvegarde et le chargement de chaînes de caractères en format binaire,
 * ainsi que le calcul du hachage SHA-1 et le téléchargement de fichiers avec vérification.
 */
public class FileManager {

    /**
     * Sauvegarde une chaîne de caractères dans un fichier binaire.
     * Format: [longueur (4 bytes)][données]
     *
     * @param data La chaîne à sauvegarder
     * @throws IOException En cas d'erreur d'écriture
     */
    public static void saveBinary(String data, String sPath) throws IOException {
        Path path = Paths.get(sPath);
        // Créer le répertoire parent si nécessaire
        File parent = path.getParent().toFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }

        // Convertir la chaîne en tableau d'octets
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);

        try (DataOutputStream out = new DataOutputStream(
                new FileOutputStream(path.toFile()))) {
            // Écrire d'abord la longueur (entier sur 4 bytes)
            out.writeInt(bytes.length);
            // Puis écrire les données
            out.write(bytes);
        }
    }

    /**
     * Charge une chaîne de caractères depuis un fichier binaire.
     *
     * @return La chaîne chargée ou null si le fichier n'existe pas
     * @throws IOException En cas d'erreur de lecture
     */
    public static String loadBinary(String sPath) throws IOException {
        Path path = Paths.get(sPath);
        File file = path.toFile();
        if (!file.exists()) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(
                new FileInputStream(file))) {
            // Lire la longueur
            int length = in.readInt();
            // Lire les données
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            // Convertir en chaîne
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }


    /**
     * Calcule et vérifie le hachage SHA-1 d'un fichier.
     *
     * @return Le hachage SHA-1 calculé ou null en cas d'erreur
     * @throws IOException En cas d'erreur de lecture
     */
    public static String calculSHA1(File file) throws IOException {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            try (InputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
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

            String calculatedSha1 = hexString.toString();

            return calculatedSha1;

        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Algorithme SHA-1 non disponible", e);
        }
    }


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
                Printer.error("Échec du téléchargement: code " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            Printer.error("Erreur lors du téléchargement: " + e.getMessage());
            return null;
        }
    }

    /**
     * Télécharge un fichier depuis une URL et vérifie son hachage SHA-1.
     *
     * @param url URL du fichier à télécharger
     * @param destinationPath Chemin local où enregistrer le fichier
     * @param expectedSha1 Hachage SHA-1 attendu pour le fichier
     * @return true si le téléchargement et la vérification réussissent, false sinon
     */
    public static boolean downloadAndVerifyFile(String url, String destinationPath, String expectedSha1) {
        File downloadedFile = downloadFile(url, destinationPath);
        if (downloadedFile == null) {
            return false; // Échec du téléchargement
        }

        try {
            String calculatedSha1 = calculSHA1(downloadedFile);
            if (calculatedSha1 != null && calculatedSha1.equals(expectedSha1)) {
                return true; // Vérification réussie
            } else {
                downloadedFile.delete(); // Supprimer le fichier en cas d'échec de vérification
                return false; // Vérification échouée
            }
        } catch (IOException e) {
            e.printStackTrace();
            downloadedFile.delete(); // Supprimer le fichier en cas d'erreur
            return false; // Erreur lors du calcul du hachage
        }
    }

    // ... tes autres méthodes (downloadFile, downloadAndVerifyFile, etc.)

    /**
     * Décompresse un fichier ZIP (ou JAR) dans le dossier de destination.
     *
     * @param zipFile  fichier .zip/.jar à extraire
     * @param destDir  répertoire de destination
     * @throws IOException si une erreur d’E/S survient
     */
    public static void unzip(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            Files.createDirectories(destDir.toPath());
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path newFilePath = destDir.toPath().resolve(entry.getName()).normalize();

                // ⚠️ Sécurité : empêcher les chemins malicieux (ZIP Slip)
                if (!newFilePath.startsWith(destDir.toPath())) {
                    throw new IOException("Entrée ZIP invalide : " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(newFilePath);
                } else {
                    Files.createDirectories(newFilePath.getParent());
                    try (OutputStream fos = Files.newOutputStream(newFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }


}
