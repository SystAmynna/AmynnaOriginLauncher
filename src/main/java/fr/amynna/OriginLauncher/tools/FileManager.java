package fr.amynna.OriginLauncher.tools;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

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


}
