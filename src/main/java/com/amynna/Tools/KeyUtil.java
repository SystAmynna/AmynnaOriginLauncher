package com.amynna.Tools;

import com.amynna.OriginBootstrap.App;

import java.io.File;
import java.nio.file.Files;
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
     * Liste des clés publiques de confiance pour valider les signatures des fichiers.
     */
    private static final Map<String, String> TRUSTED_PUBLIC_KEYS = new HashMap<>();

    /**
     * Algorithme de signature utilisé (Ed25519).
     */
    private static final String KEY_ALGORITHM = "Ed25519";

    /**
     * Initialise le gestionnaire de clés avec l'application donnée.
     * @param app Instance de l'application contenant les configurations.
     */
    public static void init(App app) {
        TRUSTED_PUBLIC_KEYS.put(AppProperties.DEFAULT_PUBLIC_KEY, AppProperties.DEFAULT_PUBLIC_KEY_OWNER);

        // Télécharger le fichier des clés publiques de confiance

        String trustedKeysFileName = "trusted-keys";
        String trustedKeysFileUrl = AppProperties.REPO_SERVER_URL + File.separator + trustedKeysFileName;

        File trustedKeysFile = FileManager.downloadAndValidateFile(trustedKeysFileUrl, trustedKeysFileName);

        // Lire le fichier des clés publiques de confiance et extraire les clés publiques

        Map<String, String> trustedKeysFileContent = FileManager.readKeyValueTextFile(trustedKeysFile);
        if (trustedKeysFileContent.isEmpty()) {
            Logger.error("⚠️  Le fichier des clés publiques de confiance est vide ou invalide.");
            return;
        }

        for (Map.Entry<String, String> entry : trustedKeysFileContent.entrySet()) {
            String keyName = entry.getKey();
            String keyValue = entry.getValue();
            TRUSTED_PUBLIC_KEYS.put(keyValue, keyName);

        }

        // Lister les clés publiques de confiance chargées

        for (Map.Entry<String, String> entry : TRUSTED_PUBLIC_KEYS.entrySet()) {
            System.out.println("🔑 Clé publique de confiance : " + entry.getValue());
        }

    }

    /**
     * Valide la signature d'un fichier en utilisant les clés publiques de confiance.
     * @param signedFile Le fichier à valider.
     * @return true si la signature est valide avec au moins une clé publique de confiance, false sinon.
     */
    public static boolean validateSignature(SignedFile signedFile) {

        for (String publicKey : TRUSTED_PUBLIC_KEYS.keySet()) {
            if (verifyFile(signedFile, publicKey )) {
                Logger.log("✅ Fichier [" + signedFile.file.getName() +
                        "] signé par [" + signedFile.signature.getName() +
                        "] validé avec la clé publique de confiance : " + TRUSTED_PUBLIC_KEYS.get(publicKey));
                return true;
            }
        }
        Logger.log("❌ Fichier [" + signedFile.file.getName() +
                "] non signé par [" + signedFile.signature.getName() +
                "] avec aucune clé publique de confiance.");
        return false;
    }


    /**
     * Génère une paire de clés publique/privée et les sauvegarde dans des fichiers.
     */
    public static void generateKeys() {

        String privatePath = "private.key";
        String publicPath = "public.key";

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            KeyPair kp = kpg.generateKeyPair();

            // Sauvegarde des clés en Base64
            Files.write(Paths.get(privatePath),
                    Base64.getEncoder().encode(kp.getPrivate().getEncoded()));
            Files.write(Paths.get(publicPath),
                    Base64.getEncoder().encode(kp.getPublic().getEncoded()));
        } catch (Exception e) {
            Logger.error("Erreur lors de la génération des clés : " + e.getMessage());
            return;
        }
        Logger.log("🔑 Clés privée et publique générées.");
    }

    /**
     * Charge une clé depuis un fichier et la retourne sous forme de chaîne.
     * @param keyFile Le fichier contenant la clé.
     * @return La clé sous forme de chaîne, ou null en cas d'erreur.
     */
    public static String loadKeyAsString(File keyFile) {
        try {
            return new String(Files.readAllBytes(keyFile.toPath()));
        } catch (Exception e) {
            Logger.error("Impossible de convertir le fichier clé en String: " + e.getMessage());
            return null;
        }
    }

    /**
     * Conversion String -> PrivateKey
     * @param base64 La clé privée en format base64.
     * @return La clé privée.
     */
    private static PrivateKey privateKeyFromString(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            Logger.error("Impossible de convertir la clé privée depuis String: " + e.getMessage());
            return null;
        }

    }
    /**
     * Conversion String -> PublicKey
     * @param base64 La clé publique en format base64.
     * @return La clé publique.
     */
    private static PublicKey publicKeyFromString(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            Logger.error("Impossible de convertir la clé publique depuis String: " + e.getMessage());
            return null;
        }

    }

    /**
     * Signe un fichier avec une clé privée et sauvegarde la signature dans un fichier séparé.
     * @param filePath Le chemin vers le fichier à signer.
     * @param privateKeyBase64 La clé privée en format base64 utilisée pour la signature.
     */
    public static void signFile(String filePath, String privateKeyBase64) {
        try {
            byte[] data = Files.readAllBytes(Paths.get(filePath));

            PrivateKey privateKey = privateKeyFromString(privateKeyBase64);

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
     * @param publicKeyBase64 La clé publique en format base64 utilisée pour la vérification.
     * @return true si la signature est valide, false sinon.
     */
    public static boolean verifyFile(SignedFile signedFile, String publicKeyBase64) {
        try {
            // data du fichier
            byte[] data = Files.readAllBytes(signedFile.file.toPath());
            // data de la signature
            byte[] sigBytes = Base64.getDecoder().decode(Files.readAllBytes(signedFile.signature.toPath()));

            PublicKey publicKey = publicKeyFromString(publicKeyBase64);

            Signature sig = Signature.getInstance(KEY_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);

            return sig.verify(sigBytes);
        } catch (Exception e) {
            Logger.error("Erreur lors de la vérification de la signature : " + e.getMessage());
            return false;
        }
    }

}
