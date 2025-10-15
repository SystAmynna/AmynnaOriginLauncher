package com.amynna.Tools;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.Certificate;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Utilitaire pour la gestion des clés cryptographiques et des signatures.
 */
public final class KeyUtil {

    /**
     * Liste des clés publiques de confiance pour valider les signatures des fichiers.
     * Sous format: Alias - clé publique
     */
    private static final Map<String, PublicKey> TRUSTED_PUBLIC_KEYS = new HashMap<>();

    /**
     * Algorithme de signature utilisé (Ed25519).
     */
    private static final String KEY_ALGORITHM = "Ed25519";

    /**
     * Type de KeyStore utilisé pour stocker les clés privées.
     */
    private static final String KEY_STORE_TYPE = "PKCS12";

    /**
     * Indicateur si le gestionnaire de clés a été initialisé.
     */
    private static boolean initialized = false;

    /**
     * Initialise le gestionnaire de clés avec l'application donnée.
     */
    private static void init() {
        // Vérifier si déjà initialisé
        if (initialized) return;

        // Ajouter la clé publique de confiance prioritaire (Master Key)
        TRUSTED_PUBLIC_KEYS.put(AppProperties.DEFAULT_PUBLIC_KEY_OWNER, getPublicKeyFromString(AppProperties.DEFAULT_PUBLIC_KEY));

        // Télécharger le fichier des clés publiques de confiance
        String trustedKeysFileName = "trusted-keys";
        File trustedKeysFile = FileManager.downloadAndValidateFile(trustedKeysFileName, AppProperties.TEMP_DIR.toPath() + File.separator + trustedKeysFileName);

        // Lire le fichier des clés publiques de confiance et extraire les clés publiques
        Map<String, String> trustedKeysFileContent = FileManager.readKeyValueTextFile(trustedKeysFile);
        if (trustedKeysFileContent.isEmpty()) {
            Logger.error("⚠️  Le fichier des clés publiques de confiance est vide ou invalide.");
            return;
        }
        for (Map.Entry<String, String> entry : trustedKeysFileContent.entrySet()) {
            String keyName = entry.getKey();
            String keyValue = entry.getValue();
            TRUSTED_PUBLIC_KEYS.put(keyName, getPublicKeyFromString(keyValue));
        }

        // Lister les clés publiques de confiance chargées
        Logger.log("🔐 Clé publique de confiance prioritaire (Master Key) : " + Logger.BOLD +
                AppProperties.DEFAULT_PUBLIC_KEY_OWNER + Logger.RESET);
        StringBuilder keysList = new StringBuilder();
        for (Map.Entry<String, PublicKey> entry : TRUSTED_PUBLIC_KEYS.entrySet()) {
            if (entry.getKey().equals(AppProperties.DEFAULT_PUBLIC_KEY_OWNER)) continue;
            keysList.append(entry.getKey()).append("  ");
        }
        Logger.log("🔑 Clés publiques de confiance (Certifiées par la Master Key) : " + Logger.BOLD + keysList + Logger.RESET);

        // Marquer comme initialisé
        initialized = true;
    }

    /**
     * Valide la signature d'un fichier en utilisant les clés publiques de confiance.
     * @param signedFile Le fichier à valider.
     * @return true si la signature est valide avec au moins une clé publique de confiance, false sinon.
     */
    public static boolean validateSignature(SignedFile signedFile) {
        // Initialiser le gestionnaire de clés si nécessaire
        init();

        // Vérifier la signature avec chaque clé publique de confiance
        for (String iPublicKey : TRUSTED_PUBLIC_KEYS.keySet()) {
            if (verifyFile(signedFile, TRUSTED_PUBLIC_KEYS.get(iPublicKey))) {
                Logger.log("✅ Fichier [" + signedFile.file.getName() +
                        "] signé par [" + signedFile.signature.getName() +
                        "] validé avec la clé publique de confiance : " + iPublicKey);
                return true;
            }
        }
        Logger.log("❌ Fichier [" + signedFile.file.getName() +
                "] non signé par [" + signedFile.signature.getName() +
                "] avec aucune clé publique de confiance.");
        return false;
    }


    /**
     * Sauvegarde une clé privée dans un KeyStore protégé par mot de passe.
     * @param privateKey La clé privée à sauvegarder.
     * @param alias L'alias sous lequel sauvegarder la clé.
     * @param password Le mot de passe pour protéger le KeyStore.
     */
    private static void savePrivateKey(PrivateKey privateKey, String alias, String password) {
        try {
            // Créer ou charger le KeyStore
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;
            if (ksFile.exists()) {
                try (FileInputStream fis = new FileInputStream(ksFile)) {
                    keyStore.load(fis, password.toCharArray());
                }
            } else {
                keyStore.load(null, null); // Nouveau KeyStore vide
            }

            // Créer l'entrée avec la clé privée
            KeyStore.PrivateKeyEntry entry = new KeyStore.PrivateKeyEntry(
                privateKey,
                new Certificate[0]
            );

            // Sauvegarder avec protection par mot de passe
            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(
                password.toCharArray()
            );
            keyStore.setEntry(alias, entry, protection);

            // Écrire sur disque
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                keyStore.store(fos, password.toCharArray());
            }

            Logger.log("🔒 Clé privée sauvegardée dans KeyStore");
        } catch (Exception e) {
            Logger.error("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    /**
     * Charge une clé privée depuis un KeyStore protégé par mot de passe.
     * @param keystorePath Le chemin vers le fichier du KeyStore.
     * @param alias L'alias de la clé à charger.
     * @param password Le mot de passe pour accéder au KeyStore.
     * @return La clé privée, ou null en cas d'erreur.
     */
    public static PrivateKey loadPrivateKey(String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            File keystoreFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;

            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(
                password.toCharArray()
            );

            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)
                keyStore.getEntry(alias, protection);

            if (entry == null) {
                Logger.error("Aucune clé trouvée pour l'alias : " + alias);
                return null;
            }

            return entry.getPrivateKey();
        } catch (Exception e) {
            Logger.error("Erreur lors du chargement : " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrait la clé publique à partir d'une clé privée.
     * @param privateKey La clé privée.
     * @return La clé publique correspondante, ou null en cas d'erreur.
     */
    public static PublicKey getPublicKeyFromPrivateKey(PrivateKey privateKey) {
        try {
            KeyFactory kf = KeyFactory.getInstance(KEY_ALGORITHM);
            // Pour Ed25519, la clé publique peut être dérivée
            return kf.generatePublic(new X509EncodedKeySpec(privateKey.getEncoded()));
        } catch (Exception e) {
            Logger.error("Erreur lors de l'extraction : " + e.getMessage());
            return null;
        }
    }


    /**
     * Génère une paire de clés publique/privée et sauvegarde la clé privée dans le KeyStore.
     * @return La clé privée générée, ou null en cas d'erreur.
     */
    public static void generatePrivateKey(String alias) {
        try {

            if (alias == null || alias.trim().isEmpty()) {
                Logger.error("❌ Alias requis pour générer une clé.");
                return;
            }

            // Demander un mot de passe pour protéger la clé
            String password = Asker.askFirstPassword();

            // Générer la paire de clés
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            KeyPair keyPair = kpg.generateKeyPair();

            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Sauvegarder la clé privée dans le KeyStore
            savePrivateKey(privateKey, alias, password);

            // Afficher la clé publique (pour distribution)
            Logger.log("🔑 Clé publique générée (à partager) :");
            Logger.log(getPublicKeyAsString(publicKey));

        } catch (Exception e) {
            Logger.error("❌ Erreur lors de la génération de la clé : " + e.getMessage());
        }
    }

    /**
     * Convertit une clé publique en une chaîne Base64.
     * @param publicKey La clé publique.
     * @return La représentation Base64 de la clé publique.
     */
    public static String getPublicKeyAsString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Convertit une chaîne Base64 en une clé publique.
     * @param publicKeyStr La chaîne Base64 de la clé publique.
     * @return La clé publique, ou null en cas d'erreur.
     */
    public static PublicKey getPublicKeyFromString(String publicKeyStr) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance(KEY_ALGORITHM);
            return kf.generatePublic(spec);
        } catch (Exception e) {
            Logger.error("Erreur lors de la conversion de la clé publique : " + e.getMessage());
            return null;
        }
    }


    /**
     * Signe un fichier avec une clé privée et sauvegarde la signature dans un fichier séparé.
     * @param filePath Le chemin vers le fichier à signer.
     * @param privateKey La clé privée.
     */
    public static void signFile(String filePath, PrivateKey privateKey) {

        try {
            byte[] data = Files.readAllBytes(Paths.get(filePath));

            Signature sig = Signature.getInstance(KEY_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(data);
            byte[] sigBytes = sig.sign();

            String sigFile = filePath + ".sig";
            Files.write(Paths.get(sigFile), Base64.getEncoder().encode(sigBytes));

            Logger.log("✅ Signature générée : " + sigFile);
        } catch (Exception e) {
            Logger.error("Erreur lors de la signature du fichier : " + e.getMessage());
        }
    }

    /**
     * Vérifie la signature d'un fichier avec une clé publique donnée.
     * @param signedFile Le fichier signé.
     * @param publicKey La clé publique.
     * @return true si la signature est valide, false sinon.
     */
    public static boolean verifyFile(SignedFile signedFile, PublicKey publicKey) {

        try {
            // data du fichier
            byte[] data = Files.readAllBytes(signedFile.file.toPath());
            // data de la signature
            byte[] sigBytes = Base64.getDecoder().decode(Files.readAllBytes(signedFile.signature.toPath()));

            Signature sig = Signature.getInstance(KEY_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);

            return sig.verify(sigBytes);
        } catch (Exception e) {
            Logger.error("Erreur lors de la vérification de la signature : " + e.getMessage());
            return false;
        }
    }

    /**
     * Donne le chemin complet du fichier de signature pour un fichier donné.
     * @param filename Le nom du fichier (sans extension).
     * @return Le chemin complet du fichier de signature.
     */
    public static String getSignaturePath(String filename) {
        return AppProperties.SIGNATURE_DIR + filename + AppProperties.SIGNATURE_FILE_EXTENSION;
    }

}
