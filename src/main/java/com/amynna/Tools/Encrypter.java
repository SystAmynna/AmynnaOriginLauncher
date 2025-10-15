package com.amynna.Tools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.security.spec.KeySpec;


public class Encrypter {

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
     * Encrypte une clé privée avec un mot de passe.
     * @param privateKey La clé privée à encrypter (en format encodé)
     * @param password Le mot de passe d'encryption
     * @return Un tableau de bytes contenant [iv + sel + données chiffrées], ou null en cas d'erreur
     */
    public static byte[] encryptPrivateKey(byte[] privateKey, String password) {
        try {
            // Générer un sel aléatoire pour PBKDF2
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Dériver une clé de chiffrement à partir du mot de passe
            int iterations = 10000;
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey tmp = factory.generateSecret(keySpec);
            SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            // Initialiser le chiffrement
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[12]; // GCM recommande 12 octets pour l'IV
            random.nextBytes(iv);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Chiffrer la clé privée
            byte[] encryptedData = cipher.doFinal(privateKey);

            // Concaténer IV + sel + données chiffrées pour stockage
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(iv);
            outputStream.write(salt);
            outputStream.write(encryptedData);

            return outputStream.toByteArray();
        } catch (Exception e) {
            Logger.error("Erreur lors de l'encryption de la clé privée: " + e.getMessage());
            return null;
        }
    }

    /**
     * Décrypte une clé privée avec un mot de passe.
     * @param encryptedData Les données chiffrées (iv + sel + clé chiffrée)
     * @param password Le mot de passe de décryption
     * @return La clé privée déchiffrée, ou null en cas d'erreur
     */
    public static byte[] decryptPrivateKey(byte[] encryptedData, String password) {
        try {
            // Extraire IV, sel et données chiffrées
            ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedData);
            byte[] iv = new byte[12];
            byte[] salt = new byte[16];
            inputStream.read(iv);
            inputStream.read(salt);
            byte[] encrypted = inputStream.readAllBytes();

            // Dériver la clé de déchiffrement
            int iterations = 10000;
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            SecretKey tmp = factory.generateSecret(keySpec);
            SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            // Déchiffrer
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            Logger.error("Erreur lors de la décryption de la clé privée: " + e.getMessage());
            return null;
        }
    }


}
