package com.amynna.OriginBootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Utilitaire pour la gestion des clés cryptographiques et des signatures.
 */
public final class KeyUtil {

    /**
     * Instance de l'application contenant les configurations.
     */
    private static App APP;

    /**
     * Clé publique par défaut pour valider les signatures des fichiers de clés publiques de confiance.
     */
    private static final String DEFAULT_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAm33YXcr/8sf5icM+WP7/XvdJKOjCXurcnN++kE7RBQmI+vOpbR5BIZNnRfo4FeVYRGd7shBd9ASAjjZjHQAfC7EyU91NMNUjCCQPgvavUnRv7F7wyDkDCCsUTBHHG0egkYBRysOIilTLLclUnseBvvmdaQ+JS7RFuLgsc5G96+F14DEPp8kZFCpY8MR/NgJeN/XQzC4+mFlHAaoU6l81Y4E2sdF4kKKuSVEpWkfoCFlLKwR22dCQEUnAn0U93EJfIKQb5cyX5oTAN42B/Qn5jkbc8QElWBh8IrJmIi+mlVqd0ioCWBlMpAee/AlLx8aTvZpdcrjN4LzwgudL01WDlQIDAQAB";
    /**
     * Nom associé à la clé publique par défaut.
     */
    private static final String DEFAULT_PUBLIC_KEY_NAME = "Stiles";

    /**
     * Liste des clés publiques de confiance pour valider les signatures des fichiers.
     */
    private static final Map<String, String> TRUSTED_PUBLIC_KEYS = new HashMap<>();

    // TODO : REFAIRE LA GESTION DES SIGNATURES ET CLÉS
    public static final int RSA_SIGNATURE_SIZE = 256;
    public static final int RSA_ENCRYPTION_KEY_SIZE = 2048;

    /**
     * Initialise le gestionnaire de clés avec l'application donnée.
     * @param app Instance de l'application contenant les configurations.
     */
    public static void init(App app) {
        TRUSTED_PUBLIC_KEYS.put(DEFAULT_PUBLIC_KEY, DEFAULT_PUBLIC_KEY_NAME);
        APP = app;

        // Télécharger le fichier des clés publiques de confiance

        String trustedKeysFileName = "trusted-keys";
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

        File unsignedTrustedKeysFile = FileManager.extractOriginalDataToFile(trustedKeysFile, app.TEMP_DIR);

        Map<String, String> trustedKeysFileContent = FileManager.readKeyValueTextFile(unsignedTrustedKeysFile);
        if (trustedKeysFileContent.isEmpty()) {
            System.err.println("⚠️  Le fichier des clés publiques de confiance est vide ou invalide.");
            return;
        }

        for (Map.Entry<String, String> entry : trustedKeysFileContent.entrySet()) {
            String keyName = entry.getKey();
            String keyValue = entry.getValue();
            TRUSTED_PUBLIC_KEYS.put(keyValue, keyName);

        }


        for (Map.Entry<String, String> entry : TRUSTED_PUBLIC_KEYS.entrySet()) {
            System.out.println("🔑 Clé publique de confiance : " + entry.getValue());
        }

    }

    /**
     * Valide la signature d'un fichier en utilisant les clés publiques de confiance.
     * @param file Le fichier à valider.
     * @return true si la signature est valide avec au moins une clé publique de confiance, false sinon.
     */
    public static boolean validateSignature(File file) {

        for (String publicKey : TRUSTED_PUBLIC_KEYS.keySet()) {
            if (verifySignature(file, publicKey)) {
                System.out.println("✅ Fichier [" + file.getName() + "] signé avec la clé publique de confiance : " + TRUSTED_PUBLIC_KEYS.get(publicKey));
                return true;
            }
        }
        System.out.println("❌ Fichier [" + file.getName() + "] non signé avec une clé publique de confiance.");
        return false;
    }

    /**
     * Signe un fichier avec une clé privée et sauvegarde le résultat.
     * @param filePath Le chemin vers le fichier à signer.
     * @param privateKeyPath Le chemin vers la clé privée (format PKCS#8).
     */
    public static void signFile(String filePath, String privateKeyPath) {

        // TODO : CHANGER LA MÉTHODE DE SIGNATURE

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

    /**
     * Génère une paire de clés RSA et les sauvegarde dans des fichiers.
     * La clé privée est sauvegardée dans "private.key" et la clé publique dans "public.key".
     */
    public static void generateKeys() {

        // TODO : CHANGER LA MÉTHODE DE GÉNÉRATION DES CLÉS

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

    /**
     * Vérifie la signature d'un fichier signé avec une clé publique donnée.
     * @param signedFile Le fichier signé à vérifier.
     * @param publicKeyBase64 La clé publique en format base64 utilisée pour la vérification.
     * @return true si la signature est valide, false sinon.
     */
    public static boolean verifySignature(File signedFile, String publicKeyBase64) {

        // TODO : CHANGER LA MÉTHODE DE VÉRIFICATION

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

    /**
     * Lit une clé depuis un fichier et la retourne sous forme de chaîne base64.
     * @param KeyPath Le chemin vers le fichier contenant la clé.
     * @return La clé en format base64, ou une chaîne vide en cas d'erreur.
     */
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
