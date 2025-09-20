package com.amynna.OriginBootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;

public final class KeyUtil {

    private static App APP;

    private static final String DEFAULT_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm33YXcr/8sf5icM+WP7/XvdJKOjCXurcnN++kE7RBQmI+vOpbR5BIZNnRfo4FeVYRGd7shBd9ASAjjZjHQAfC7EyU91NMNUjCCQPgvavUnRv7F7wyDkDCCsUTBHHG0egkYBRysOIilTLLclUnseBvvmdaQ+JS7RFuLgsc5G96+F14DEPp8kZFCpY8MR/NgJeN/XQzC4+mFlHAaoU6l81Y4E2sdF4kKKuSVEpWkfoCFlLKwR22dCQEUnAn0U93EJfIKQb5cyX5oTAN42B/Qn5jkbc8QElWBh8IrJmIi+mlVqd0ioCWBlMpAee/AlLx8aTvZpdcrjN4LzwgudL01WDlQIDAQAB";

    private static final LinkedList<String> TRUSTED_PUBLIC_KEYS = new LinkedList<>();

    // La taille de la signature dépend de la taille de la clé. Pour une clé RSA 2048 bits, elle est de 256 octets.
    private static final int RSA_SIGNATURE_SIZE = 256;
    private static final int RSA_ENCRYPTION_KEY_SIZE = 2048;


    public static void init(App app) {
        TRUSTED_PUBLIC_KEYS.add(DEFAULT_PUBLIC_KEY);
        APP = app;

        // Télécharger le fichier des clés publiques de confiance

        String trustedKeysFileName = "trusted-keys.json";
        String trustedKeysFileUrl = APP.SERVER_URL + File.separator + trustedKeysFileName;

        File trustedKeysFile = FileManager.downloadFile(trustedKeysFileUrl, APP.LAUNCHER_ROOT + trustedKeysFileName);

        if (trustedKeysFile == null || !trustedKeysFile.exists()) {
            System.err.println("⚠️  Impossible de télécharger le fichier des clés publiques de confiance.");
            return;
        }

        // Valider les clés publiques de confiance avec la clé publique par défaut

        if (!validateSignature(trustedKeysFile)) {
            System.err.println("⚠️  Le fichier des clés publiques de confiance n'est pas signé avec la clé publique par défaut.");
            return;
        }

        // Lire le fichier des clés publiques de confiance et extraire les clés publiques

        Map<String, String> trustedKeysFileContent = FileManager.readKeyValueTextFile(trustedKeysFile);
        if (trustedKeysFileContent == null || trustedKeysFileContent.isEmpty()) {
            System.err.println("⚠️  Le fichier des clés publiques de confiance est vide ou invalide.");
            return;
        }

        for (Map.Entry<String, String> entry : trustedKeysFileContent.entrySet()) {
            String keyName = entry.getKey();
            String keyValue = entry.getValue();
            TRUSTED_PUBLIC_KEYS.add(keyValue);

        }

    }


    public static boolean validateSignature(File launcherFile) {

        for (String publicKey : TRUSTED_PUBLIC_KEYS) {
            if (verifySignature(launcherFile, publicKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Signe un fichier avec une clé privée et sauvegarde le résultat.
     * @param filePath Le chemin vers le fichier à signer.
     * @param privateKeyPath Le chemin vers la clé privée (format PKCS#8).
     */
    public static void signFile(String filePath, String privateKeyPath) {

        try {

            // Charger la clé privée
            byte[] keyBytes = Files.readAllBytes(Paths.get(privateKeyPath));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(spec);

            // Initialiser l'objet Signature
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);

            // Lire le fichier à signer et mettre à jour l'objet Signature
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            sig.update(fileBytes);

            // Générer la signature
            byte[] signatureBytes = sig.sign();

            // Créer le nouveau fichier signé
            Path originalPath = Paths.get(filePath);
            Path signedFilePath = originalPath.getParent().resolve("signed_" + originalPath.getFileName().toString());

            try (FileOutputStream fos = new FileOutputStream(signedFilePath.toFile())) {
                // Écrire la signature puis le contenu original
                fos.write(signatureBytes);
                fos.write(fileBytes);
            }
            System.out.println("✅ Fichier signé avec succès : " + signedFilePath);

        } catch (Exception e) {
            System.err.println("Erreur lors de la signature du fichier : " + e.getMessage());
        }
    }

    public static void generateKeys() {
        try {
            // Générer une paire de clés RSA
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            // définir la taille de la clé
            kpg.initialize(RSA_ENCRYPTION_KEY_SIZE); // Taille de la clé
            // générer la paire de clés
            KeyPair kp = kpg.generateKeyPair();
            // récupérer les clés publique et privée
            PrivateKey privateKey = kp.getPrivate(); // clé privée
            PublicKey publicKey = kp.getPublic(); // clé publique

            try (FileOutputStream out = new FileOutputStream("private.key")) {
                out.write(privateKey.getEncoded());
            }
            try (FileOutputStream out = new FileOutputStream("public.key")) {
                out.write(publicKey.getEncoded());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des clés : " + e.getMessage());
            return;
        }




        System.out.println("🔑 Clés privée et publique générées.");
    }



    public static boolean verifySignature(File signedFile, String publicKeyBase64) {
        try {
            // Décoder la clé publique depuis la chaîne base64
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            // Lire le contenu du fichier signé
            byte[] signedFileBytes = Files.readAllBytes(signedFile.toPath());

            // Extraire la signature et les données originales
            byte[] signatureBytes = Arrays.copyOfRange(signedFileBytes, 0, RSA_SIGNATURE_SIZE);
            byte[] originalFileBytes = Arrays.copyOfRange(signedFileBytes, RSA_SIGNATURE_SIZE, signedFileBytes.length);

            // Initialiser l'objet Signature pour la vérification
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);

            // Fournir les données originales à l'objet Signature
            sig.update(originalFileBytes);

            // Vérifier la signature
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de la signature : " + e.getMessage());
            return false;
        }
    }

    public static String keyAsString(String KeyPath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Paths.get(KeyPath));
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture de la clé : " + e.getMessage());
        }
        return "" ;
    }



}
