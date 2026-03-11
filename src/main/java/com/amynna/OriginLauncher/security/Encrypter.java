package com.amynna.OriginLauncher.security;

import com.amynna.OriginLauncher.AppProperties;
import com.amynna.OriginLauncher.output.Logger;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class Encrypter {

    private static final String [] FILE_FORMATS_TO_NORMALIZE = {".json", ".txt", ".xml", ".yml"};

    public static String hashString(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Conversion en hexadécimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme non supporté : " + algorithm, e);
        }
    }

    public static String sha512(String input) {
        return hashString(input, "SHA-512");
    }

    /**
     * Sauvegarde un token dans le KeyStore sous forme de clé secrète.
     * @param alias L'alias sous lequel sauvegarder le token.
     * @param token Le token à sauvegarder.
     * @param password Le mot de passe du KeyStore.
     * @return true si la sauvegarde a réussi, false sinon.
     */
    public static boolean saveToken(String alias, String token, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyTool.KEY_STORE_TYPE);
            File ksFile = AppProperties.MS_AUTH_TOKEN;

            // Charger ou créer le KeyStore
            if (ksFile.exists()) {
                try (FileInputStream fis = new FileInputStream(ksFile)) {
                    keyStore.load(fis, password.toCharArray());
                }
            } else {
                keyStore.load(null, null);
            }

            // Convertir le token en SecretKey
            SecretKey secretKey = new SecretKeySpec(token.getBytes(StandardCharsets.UTF_8), "AES");

            // Créer une entrée protégée par mot de passe
            KeyStore.SecretKeyEntry secretKeyEntry = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.PasswordProtection protection =
                new KeyStore.PasswordProtection(password.toCharArray());

            // Sauvegarder l'entrée
            keyStore.setEntry(alias, secretKeyEntry, protection);

            // Sauvegarder le KeyStore
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                keyStore.store(fos, password.toCharArray());
            }

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔐 Token sauvegardé : " + alias);
            return true;

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Erreur lors de la sauvegarde du token : " + e.getMessage());
            return false;
        }
    }

    /**
     * Récupère un token du KeyStore.
     * @param alias L'alias du token à récupérer.
     * @param password Le mot de passe du KeyStore.
     * @return Le token récupéré, ou null en cas d'erreur.
     */
    public static String loadToken(String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyTool.KEY_STORE_TYPE);
            File keyStoreFile = AppProperties.MS_AUTH_TOKEN;

            if (!keyStoreFile.exists()) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ KeyStore introuvable.");
                return null;
            }

            // Charger le KeyStore
            try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Vérifier que l'alias existe
            if (!keyStore.containsAlias(alias)) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Aucun token trouvé pour l'alias : " + alias);
                return null;
            }

            // Récupérer l'entrée
            KeyStore.PasswordProtection protection =
                new KeyStore.PasswordProtection(password.toCharArray());
            KeyStore.SecretKeyEntry entry =
                (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, protection);

            // Convertir la SecretKey en String
            byte[] tokenBytes = entry.getSecretKey().getEncoded();
            String token = new String(tokenBytes, StandardCharsets.UTF_8);

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔓 Token récupéré : " + alias);
            return token;

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Erreur lors de la récupération du token : " + e.getMessage());
            return null;
        }
    }


    public static byte[] getFileBytesNormalized(File file) throws Exception {
        String name = file.getName().toLowerCase();

        for (String ext : FILE_FORMATS_TO_NORMALIZE) {
            if (name.endsWith(ext)) {
                // Lecture en tant que String
                String content = Files.readString(file.toPath());

                // REMPLACEMENT MAGIQUE : On force le LF (\n) partout
                // On remplace d'abord les CRLF (\r\n) par LF (\n) pour Windows
                // On pourrait aussi remplacer les CR seuls (\r) si on voulait être puriste (Mac OS 9)
                content = content.replace("\\R", "\n");

                return content.getBytes(StandardCharsets.UTF_8);
            }
        }
        return Files.readAllBytes(file.toPath());

    }


}
