package com.amynna.OriginLauncher.security;

import com.amynna.OriginLauncher.AppProperties;
import com.amynna.OriginLauncher.FileManager;
import com.amynna.OriginLauncher.output.Logger;

import java.io.File;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.Signature;

public class KeySigner {

    /**
     * Signe un fichier ou un répertoire avec une clé privée.
     * @param file Le fichier ou répertoire à signer.
     * @param signPath Le chemin où sauvegarder les signatures.
     * @param privateKey La clé privée.
     */
    public static SignedFile sign(File file, String signPath, PrivateKey privateKey) {

        if (file.isFile()) return signFile(file, signPath, privateKey);
        else if (file.isDirectory()) return signDirectory(file, signPath, privateKey);
        else {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, "Erreur : Le chemin spécifié n'est ni un fichier ni un répertoire valide.");
            return null;
        }

    }
    /**
     * Signe un fichier ou un répertoire avec une clé privée.
     * @param file Le fichier ou répertoire à signer.
     * @param privateKey La clé privée.
     */
    public static SignedFile sign(File file, PrivateKey privateKey) {
        return sign(file, "", privateKey);
    }


    /**
     * Signe un fichier avec une clé privée et sauvegarde la signature dans un fichier séparé.
     * @param file Le fichier à signer.
     * @param signPath Le chemin où sauvegarder le fichier de signature.
     * @param privateKey La clé privée.
     */
    private static SignedFile signFile(File file, String signPath, PrivateKey privateKey) {

        // Vérifier que le fichier existe
        if (file == null || !file.exists() || !file.isFile()) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Erreur : Le fichier à signer est introuvable.");
            return null;
        }

        // vérifier le chemin de sauvegarde
        String signFilePath = signPath;
        if (!(signFilePath == null || signFilePath.isEmpty()) && !signFilePath.endsWith("/")) signFilePath += File.separator;
        signFilePath += file.getName() + AppProperties.SIGNATURE_FILE_EXTENSION;

        // Supprimer l'ancien fichier de signature s'il existe
        File signFile = new File(signFilePath);
        FileManager.deleteFileIfExists(signFile);

        // Signer le fichier
        try {
            // Lire le contenu du fichier
            byte[] data = Encrypter.getFileBytesNormalized(file);

            // Signer les données
            Signature sig = Signature.getInstance(KeyTool.KEY_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(data);
            byte[] sigBytes = sig.sign();

            // Sauvegarder la signature dans un fichier (encodée en Base64)
            Files.write(signFile.toPath(), sigBytes);

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Signature générée : " + signFilePath);
            return new SignedFile(file, signFile);
        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors de la signature du fichier : " + e.getMessage());
            return null;
        }
    }
    /**
     * Signe un fichier avec une clé privée et sauvegarde la signature dans un fichier séparé.
     * @param file Le fichier à signer.
     * @param privateKey La clé privée.
     */
    private static SignedFile signFile(File file, PrivateKey privateKey) {
        return signFile(file, "", privateKey);
    }

    /**
     * Signe tous les fichiers d'un répertoire avec une clé privée.
     * @param dir Le répertoire à signer.
     * @param signDirectoryPath Le chemin où sauvegarder le répertoire de signatures.
     * @param privateKey La clé privée.
     */
    private static SignedFile signDirectory(File dir, String signDirectoryPath, PrivateKey privateKey) {

        // Vérifier que le répertoire existe
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Le répertoire à signer est introuvable.");
            return null;
        }

        // Vérifier le chemin de sauvegarde
        if (!(signDirectoryPath == null || signDirectoryPath.isEmpty()) && !signDirectoryPath.endsWith("/")) signDirectoryPath += File.separator;
        signDirectoryPath += dir.getName(); //+ AppProperties.SIGNATURE_FILE_EXTENSION;

        // Supprimer l'ancien répertoire de signatures s'il existe
        File signDirectory = new File(signDirectoryPath);
        FileManager.deleteFileIfExists(signDirectory);

        // Créer le répertoire de signatures
        FileManager.createDirectoriesIfNotExist(signDirectory.getPath());

        // Lister tous les fichiers du répertoire
        File[] files = dir.listFiles();
        if (files == null) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Impossible de lister les fichiers dans le répertoire à signer.");
            return null;
        }
        if (files.length == 0) {
            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Le répertoire à signer est vide, ignoré.");
            return new SignedFile(dir, signDirectory);
        }

        // Signer chaque fichier
        for (File file : files) {
            if (file.isFile()) signFile(file, signDirectoryPath, privateKey);
            else if (file.isDirectory()) signDirectory(file, signDirectoryPath, privateKey);
            else Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Ignoré (fichier ou repertoire invalide) : " + file.getName());
        }

        return new SignedFile(dir, signDirectory);
    }
    /**
     * Signe tous les fichiers d'un répertoire avec une clé privée.
     * @param dir Le répertoire à signer.
     * @param privateKey La clé privée.
     */
    private static SignedFile signDirectory(File dir, PrivateKey privateKey) {
        return signDirectory(dir, "", privateKey);
    }

}
