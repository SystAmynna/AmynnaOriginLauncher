package com.amynna.Tools;

import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
            Logger.log(Logger.BLUE + "\uD83D\uDCE5 Fichier téléchargé : " + url + " ➔ " + downloadedFile.getAbsolutePath());

            return downloadedFile;

        } catch (Exception e) {
            Logger.error("Erreur lors du téléchargement du fichier " + url + " : " + e.getMessage());
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
                if (!line.contains(":")) continue;
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
            if (!dir.mkdirs()) {
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

    /**
     * Ouvre un fichier JSON et le charge dans un objet JSONObject.
     *
     * @param file Fichier JSON à ouvrir
     * @return Un objet JSONObject contenant les données du fichier, ou null en cas d'erreur
     */
    public static JSONObject openJsonFile(File file) {

        if (!file.exists()) {
            Logger.error("Le fichier JSON n'existe pas : " + file.getPath());
            return null;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return new JSONObject(content);
        } catch (IOException e) {
            Logger.error("Erreur lors de la lecture du fichier JSON : " + e.getMessage());
            return null;
        }
    }

    /**
     * Calcule et vérifie le hachage SHA-1 d'un fichier.
     *
     * @return Le hachage SHA-1 calculé ou null en cas d'erreur
     * @throws IOException En cas d'erreur de lecture
     */
    public static String calculSHA1(File file) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");

            try (InputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                Logger.error("Erreur de lecture du fichier pour le calcul SHA-1 : " + e.getMessage());
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
            Logger.fatal("Algorithme SHA-1 non trouvé : " + e.getMessage());
            return null;
        }
    }

    /**
     * Calcule le hachage SHA-256 d'un fichier.
     *
     * @param file Fichier à hasher
     * @return Le hachage SHA-256 en hexadécimal ou null en cas d'erreur
     */
    public static String calculSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                Logger.error("Erreur de lecture du fichier pour le calcul SHA-256 : " + e.getMessage());
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
            Logger.fatal("Algorithme SHA-256 non trouvé : " + e.getMessage());
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
    public static File downloadFileAndVerifySha1(String url, String destinationPath, String expectedSha1) {
        File downloadedFile = downloadFile(url, destinationPath);
        if (downloadedFile == null) {
            return null; // Échec du téléchargement
        }

        try {
            String calculatedSha1 = calculSHA1(downloadedFile);
            if (calculatedSha1 != null && calculatedSha1.equals(expectedSha1)) {
                return downloadedFile; // Vérification réussie
            }
        } catch (SecurityException e) {
            Logger.error("Erreur de sécurité lors de la vérification SHA-1 : " + e.getMessage());
        }

        deleteFileIfExists(downloadedFile);
        return null;

    }

    /**
     * Télécharge un fichier depuis une URL et vérifie son hachage SHA-256.
     *
     * @param url URL du fichier à télécharger
     * @param destinationPath Chemin local où enregistrer le fichier
     * @param expectedSha256 Hachage SHA-256 attendu pour le fichier
     * @return true si le téléchargement et la vérification réussissent, false sinon
     */
    public static File downloadFileAndVerifySha256(String url, String destinationPath, String expectedSha256) {
        File downloadedFile = downloadFile(url, destinationPath);
        if (downloadedFile == null) {
            return null; // Échec du téléchargement
        }

        try {
            String calculatedSha256 = calculSHA256(downloadedFile);
            if (calculatedSha256 != null && calculatedSha256.equals(expectedSha256)) {
                return downloadedFile; // Vérification réussie
            }
        } catch (SecurityException e) {
            Logger.error("Erreur de sécurité lors de la vérification SHA-256 : " + e.getMessage());
        }

        deleteFileIfExists(downloadedFile);
        return null;

    }


    /**
     * Vérifie la taille d'un fichier sur le disque.
     * @param file Le fichier à vérifier.
     * @param expectedSize La taille attendue en octets.
     * @return true si la taille correspond, false sinon.
     * @throws IOException Si le fichier n'existe pas ou ne peut pas être lu.
     */
    public static boolean verifyFileSize(File file, long expectedSize){
        if (!file.exists()) {
            return false;
        }
        long actualSize = 0;
        try {
            actualSize = Files.size(file.toPath());
        } catch (IOException e) {
            Logger.error("Erreur lors de la vérification de la taille du fichier : " + e.getMessage());
            return false;
        }
        // Une tolérance de 100 octets est ajoutée pour compenser les différences potentielles de métadonnées,
        // mais le SHA1 reste la méthode de vérification principale.
        return Math.abs(actualSize - expectedSize) <= 100;
    }

/**
 * Supprime un fichier ou un répertoire (et son contenu) s'il existe.
 *
 * @param file Fichier ou répertoire à supprimer
 */
public static void deleteFileIfExists(File file) {
    if (!file.exists()) {
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
        Logger.error("Impossible de supprimer : " + file.getPath());
    }
}

    /**
     * Décompresse un fichier ZIP (ou JAR) dans le dossier de destination.
     *
     * @param zipFile  fichier .zip/.jar à extraire
     * @param destDir  répertoire de destination
     */
    public static void unzip(File zipFile, File destDir) {

        // Crée le répertoire de destination s'il n'existe pas
        createDirectoriesIfNotExist(destDir.getPath());


        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                Path newFilePath = destDir.toPath().resolve(entry.getName()).normalize();

                // ⚠️ Sécurité : empêcher les chemins malicieux (ZIP Slip)
                if (!newFilePath.startsWith(destDir.toPath())) {
                    zis.closeEntry();
                    Logger.error("Entrée ZIP malicieuse détectée : " + entry.getName() + " - extraction ignorée.");
                    continue;
                }

                if (entry.isDirectory()) {
                    createDirectoriesIfNotExist(newFilePath.toString());
                } else {
                    createDirectoriesIfNotExist(newFilePath.getParent().toString());
                    try (OutputStream fos = Files.newOutputStream(newFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (FileNotFoundException e) {
            Logger.error("Fichier ZIP non trouvé : " + e.getMessage());
        } catch (IOException e) {
            Logger.error("Erreur lors de la décompression du fichier ZIP : " + e.getMessage());
        }


    }

    /**
     * Décompresse un fichier TAR.GZ dans le dossier de destination en utilisant les outils système.
     *
     * @param tarGzFile fichier .tar.gz à extraire
     * @param destDir   répertoire de destination
     */
    public static void untarGz(File tarGzFile, File destDir) {

        // Crée le répertoire de destination s'il n'existe pas
        createDirectoriesIfNotExist(destDir.getPath());

        try {
            ProcessBuilder processBuilder;

            if (AppProperties.getOsType().equals("Windows")) {
                // Windows : utiliser tar.exe (disponible depuis Windows 10)
                processBuilder = new ProcessBuilder(
                    "tar", "-xzf",
                    tarGzFile.getAbsolutePath(),
                    "-C", destDir.getAbsolutePath()
                );
            } else {
                // Linux/macOS : utiliser tar
                processBuilder = new ProcessBuilder(
                    "tar", "-xzf",
                    tarGzFile.getAbsolutePath(),
                    "-C", destDir.getAbsolutePath()
                );
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Lire la sortie du processus
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Logger.log(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                Logger.error("Échec de la décompression TAR.GZ (code: " + exitCode + ")");
            }

        } catch (IOException e) {
            Logger.error("Erreur lors de la décompression du fichier TAR.GZ : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.error("Décompression TAR.GZ interrompue : " + e.getMessage());
        }
    }

}
