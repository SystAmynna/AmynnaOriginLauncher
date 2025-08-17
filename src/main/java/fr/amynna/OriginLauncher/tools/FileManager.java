package fr.amynna.OriginLauncher.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
     * @param filePath Le chemin du fichier à vérifier
     * @param expectedSha1 Le hachage SHA-1 attendu (peut être null pour simplement calculer le hachage)
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

    public static boolean downloadAndVerifyFile(String url, String destinationPath, String expectedSha1) {
        File downloadedFile = Web.downloadFile(url, destinationPath);
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


}
