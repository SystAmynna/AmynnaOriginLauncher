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

    public static final String SHA1 = "SHA-1";;
    public static final String SHA256 = "SHA-256";
    public static final String SHA512 = "SHA-512";


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
    public static SignedFile downloadAndValidateFile(String onServerPath, String destinationPath) {

        // Chemin du fichier et de sa signature sur le serveur
        String fileOnServerPath = AppProperties.REPO_SERVER_URL + onServerPath;

        // Télécharger le fichier principal
        File file = downloadFile(fileOnServerPath, destinationPath);
        if (file == null || !file.exists()) {
            Logger.error("Erreur lors du téléchargement du fichier...");
            return null;
        }

        // Télécharger le fichier de signature
        File signatureFile = downloadSignatureFile(onServerPath);

        SignedFile signedFile = new SignedFile(file, signatureFile);

        if (!signedFile.valid()) {
            Logger.error("Le fichier téléchargé n'est pas signé avec une clé publique de confiance.");
            signedFile.delete();
            return null;
        }
        return signedFile;
    }

    public static File downloadSignatureFile(String originalOnServerPath) {

        // Chemin du fichier de signature sur le serveur
        String signOnServerPath = AppProperties.SIGNATURE_LOCATION_ON_SERVER + originalOnServerPath + AppProperties.SIGNATURE_FILE_EXTENSION;

        // Emplacement local du fichier de signature
        String localSignPath = AppProperties.SIGNATURE_DIR.getPath() + File.separator + originalOnServerPath + AppProperties.SIGNATURE_FILE_EXTENSION;

        // Télécharger le fichier de signature
        File signatureFile = downloadFile(signOnServerPath, localSignPath);
        if (signatureFile == null || !signatureFile.exists()) {
            Logger.error("Erreur lors du téléchargement du fichier de signature...");
        }

        // Télécharger le fichier de signature
        return signatureFile;
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
            Logger.error("Erreur lors de l'écriture du fichier JSON : " + e.getMessage());
            return null;
        }
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
                Logger.error("Erreur de lecture du fichier pour le calcul " + shaType + " : " + e.getMessage());
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
            Logger.error("Erreur de sécurité lors de la vérification " + shaType + " : " + e.getMessage());
        }

        deleteFileIfExists(downloadedFile);
        return null;

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

    /**
     * Recherche un fichier par nom dans un répertoire et ses sous-répertoires.
     *
     * @param directory Répertoire de départ pour la recherche
     * @param fileName  Nom du fichier à rechercher
     * @return Le fichier trouvé, ou null si non trouvé
     */
    public static File searchFileInDirectory(File directory, String fileName) {
        if (!directory.isDirectory()) {
            Logger.error("Le chemin spécifié n'est pas un répertoire : " + directory.getPath());
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            Logger.error("Impossible de lister les fichiers dans le répertoire : " + directory.getPath());
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


    public static void renameFile(File oldFile, File newFile) {
        if (oldFile.exists()) {
            if (!oldFile.renameTo(newFile)) {
                Logger.error("Échec du renommage de " + oldFile.getPath() + " vers " + newFile.getPath());
            }
        } else {
            Logger.error("Le fichier à renommer n'existe pas : " + oldFile.getPath());
        }
    }


    public static boolean pingServer(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000); // 5 secondes
            connection.setReadTimeout(5000);    // 5 secondes
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException e) {
            return false;
        }
    }

}
