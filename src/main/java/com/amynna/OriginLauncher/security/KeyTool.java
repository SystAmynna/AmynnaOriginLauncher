package com.amynna.OriginLauncher.security;

import com.amynna.OriginLauncher.AppProperties;
import com.amynna.OriginLauncher.FileManager;
import com.amynna.OriginLauncher.output.Asker;
import com.amynna.OriginLauncher.output.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public final class KeyTool {

    /**
     * Algorithme de signature utilisé (Ed25519).
     */
    static final String KEY_ALGORITHM = "Ed25519";

    /**
     * Type de KeyStore utilisé pour stocker les clés privées.
     */
    public static final String KEY_STORE_TYPE = "PKCS12";

    /**
     * Génère une paire de clés publique/privée et sauvegarde la clé privée dans le KeyStore.
     * @param alias L'alias sous lequel sauvegarder la clé dans le KeyStore.
     */
    public static void generateKeys(String alias) {
        try {

            if (alias == null || alias.trim().isEmpty()) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, "❌ Alias requis pour générer une clé.");
                return;
            }

            // Demander un mot de passe pour protéger la clé
            String password;
            if (AppProperties.LOCAL_PRIVATE_KEYS_LOCATION.exists() && AppProperties.LOCAL_PRIVATE_KEYS_LOCATION.isFile())
                password = Asker.askPassword();
            else password = Asker.askFirstPassword();

            // Générer la paire de clés
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            KeyPair keyPair = kpg.generateKeyPair();

            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();

            // Sauvegarder la clé privée dans le KeyStore
            saveKeys(privateKey, publicKey, alias, password);

            // Afficher la clé publique (pour distribution)
            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, "🔑 Clé publique générée (à partager) :", Logger.EOL, getPublicKeyAsString(publicKey));


        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, e, "❌ Erreur lors de la génération de la clé");
        }
    }

    /**
     * Sauvegarde une paire de clés (privée + publique) dans un KeyStore PKCS12.
     * @param privateKey La clé privée à sauvegarder.
     * @param publicKey La clé publique à sauvegarder.
     * @param alias L'alias sous lequel sauvegarder les clés.
     * @param password Le mot de passe pour protéger le KeyStore.
     */
    private static void saveKeys(PrivateKey privateKey, PublicKey publicKey, String alias, String password) {
        try {
            // Créer ou charger le KeyStore
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;
            if (ksFile.exists()) {
                try (FileInputStream fis = new FileInputStream(ksFile)) {
                    keyStore.load(fis, password.toCharArray());
                }
            } else {
                keyStore.load(null, null);
                FileManager.createDirectoriesIfNotExist(ksFile.getParentFile().getPath());
            }

            // Vérifier si l'alias existe déjà
            if (keyStore.containsAlias(alias)) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, "❌ Alias déjà utilisé. Choisissez un autre alias.");
                return;
            }

            // Créer le certificat factice avec la vraie clé publique
            Certificate cert = createCertificateWithPublicKey(privateKey, publicKey);

            // Sauvegarder la clé privée avec le certificat
            keyStore.setKeyEntry(
                    alias,
                    privateKey,
                    password.toCharArray(),
                    new Certificate[]{cert}
            );

            // Écrire le KeyStore sur disque
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                keyStore.store(fos, password.toCharArray());
            }

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔒 Clés privée et publique sauvegardées, alias : " + alias);

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, e,"Erreur lors de la sauvegarde");
        }
    }

    /**
     * Supprime une paire de clés (privée + publique) du KeyStore.
     * @param alias L'alias de la clé à supprimer.
     * @param password Le mot de passe pour accéder au KeyStore.
     */
    public static void deleteKeys(String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;

            if (!ksFile.exists()) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ KeyStore introuvable.");
                return;
            }

            // Charger le KeyStore
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Vérifier que l'alias existe
            if (!keyStore.containsAlias(alias)) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Aucune clé trouvée pour l'alias : " + alias);
                return;
            }

            if (!Asker.confirmAction("Confirmer la suppression de la clé '" + alias + "' ?")) {
                Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Suppression annulée par l'utilisateur.");
                return;
            }

            // Supprimer l'entrée
            keyStore.deleteEntry(alias);

            // Sauvegarder le KeyStore modifié
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                keyStore.store(fos, password.toCharArray());
            }

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🗑️  Clé supprimée : " + alias);

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Change le mot de passe d'accès global du KeyStore.
     * @param oldPassword L'ancien mot de passe du KeyStore.
     */
    public static void changeKeyStorePassword(String oldPassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;

            if (!ksFile.exists()) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ KeyStore introuvable.");
                return;
            }

            // Charger le KeyStore avec l'ancien mot de passe
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, oldPassword.toCharArray());
            }

            // Demander le nouveau mot de passe
            String newPassword = Asker.askFirstPassword();
            if (newPassword == null ) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Nouveau mot de passe invalide.");
                return;
            }

            // Récupérer toutes les clés et les ré-encrypter avec le nouveau mot de passe
            java.util.Enumeration<String> aliases = keyStore.aliases();
            KeyStore newKeyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            newKeyStore.load(null, null);

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                if (keyStore.isKeyEntry(alias)) {
                    // Récupérer la clé privée avec l'ancien mot de passe
                    KeyStore.PasswordProtection oldProtection =
                            new KeyStore.PasswordProtection(oldPassword.toCharArray());
                    KeyStore.PrivateKeyEntry entry =
                            (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, oldProtection);

                    // Sauvegarder avec le nouveau mot de passe
                    newKeyStore.setKeyEntry(
                            alias,
                            entry.getPrivateKey(),
                            newPassword.toCharArray(),
                            entry.getCertificateChain()
                    );
                } else if (keyStore.isCertificateEntry(alias)) {
                    // Copier les certificats seuls
                    newKeyStore.setCertificateEntry(alias, keyStore.getCertificate(alias));
                }
            }

            // Sauvegarder le nouveau KeyStore avec le nouveau mot de passe
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                newKeyStore.store(fos, newPassword.toCharArray());
            }

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔐 Mot de passe du KeyStore changé avec succès");

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Erreur lors du changement de mot de passe : " + e.getMessage());
        }
    }

    /**
     * Liste toutes les clés stockées dans le KeyStore avec leurs informations.
     * @param password Le mot de passe pour accéder au KeyStore.
     * @return true si la liste a été affichée, false sinon.
     */
    public static List<String> listKeys(String password) {

        List<String> keys = new LinkedList<>();


        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;

            if (!ksFile.exists()) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ KeyStore introuvable.");
                return null;
            }

            // Charger le KeyStore
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Récupérer tous les alias
            java.util.Enumeration<String> aliases = keyStore.aliases();

            if (!aliases.hasMoreElements()) {
                Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"ℹ️  Aucune clé stockée dans le KeyStore.");
                return keys;
            }

            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔑 Clés stockées dans le KeyStore :");
            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"═══════════════════════════════════");

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                keys.add(alias);

                if (keyStore.isKeyEntry(alias)) {
                    // C'est une clé privée
                    java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    PublicKey publicKey = cert.getPublicKey();

                    printKeyInfo(alias, publicKey);
                    Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"───────────────────────────────────");

                } else if (keyStore.isCertificateEntry(alias)) {
                    // C'est uniquement un certificat
                    Certificate cert = keyStore.getCertificate(alias);
                    PublicKey publicKey = cert.getPublicKey();

                    Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"📌 Alias : " + Logger.BOLD + alias + Logger.RESET);
                    Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"   Type : Certificat seul");
                    Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"   Algorithme : " + publicKey.getAlgorithm());
                    Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"───────────────────────────────────");
                }
            }

            return keys;

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"❌ Erreur lors de la liste des clés : " + e.getMessage());
            return null;
        }
    }

    /**
     * Charge une clé privée depuis un KeyStore protégé par mot de passe.
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
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Aucune clé trouvée pour l'alias : " + alias);
                return null;
            }

            return entry.getPrivateKey();
        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors du chargement : " + e.getMessage());
            return null;
        }
    }
    /**
     * Charge la clé publique associée à une clé privée depuis le KeyStore.
     * @param alias L'alias de la clé.
     * @param password Le mot de passe du KeyStore.
     * @return La clé publique, ou null en cas d'erreur.
     */
    public static PublicKey loadPublicKey(String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);

            File keystoreFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Récupérer le certificat qui contient la clé publique
            Certificate cert = keyStore.getCertificate(alias);
            if (cert == null) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Aucun certificat trouvé pour l'alias : " + alias);
                return null;
            }

            return cert.getPublicKey();

        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Erreur lors du chargement de la clé publique : " + e.getMessage());
            return null;
        }
    }


    /**
     * Convertit une clé publique en une chaîne Base64.
     * @param publicKey La clé publique.
     * @return La représentation Base64 de la clé publique.
     */
    public static String getPublicKeyAsString(PublicKey publicKey) {
        if (publicKey == null) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.VERBOSE,"Clé publique invalide (null)");
            return "";
        }
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
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.VERBOSE,"Erreur lors de la conversion de la clé publique : " + e.getMessage());
            return null;
        }
    }

    /**
     * Donne le chemin complet du fichier de signature pour un fichier donné.
     * @param filename Le nom du fichier (sans extension).
     * @return Le chemin complet du fichier de signature.
     */
    public static File getSignatureFile(String filename) {

        String signName = filename + AppProperties.SIGNATURE_FILE_EXTENSION;

        return FileManager.searchFileInDirectory(AppProperties.SIGNATURE_DIR, signName);

    }

    /**
     * Affiche les informations d'une clé publique.
     * @param alias L'alias de la clé.
     * @param publicKey La clé publique.
     */
    public static void printKeyInfo(String alias, PublicKey publicKey) {
        Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"📌 Alias : " + Logger.BOLD + alias);
        Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"   Clé publique : " + getPublicKeyAsString(publicKey));
    }

    /**
     * Crée un certificat X.509 contenant la clé publique fournie.
     * @param privateKey La clé privée pour signer le certificat.
     * @param publicKey La clé publique à inclure dans le certificat.
     * @return Le certificat X.509 auto-signé.
     * @throws CertificateException En cas d'erreur lors de la création du certificat.
     * @throws OperatorCreationException En cas d'erreur lors de la création du ContentSigner.
     */
    private static Certificate createCertificateWithPublicKey(PrivateKey privateKey, PublicKey publicKey) throws CertificateException, OperatorCreationException {
        X500Name issuer = new X500Name("CN=" + AppProperties.APP_NAME + "-LocalKey");
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 365L * 24 * 60 * 60 * 1000 * 10); // 10 ans

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serialNumber,
                notBefore,
                notAfter,
                issuer,
                publicKey  // Utiliser la vraie clé publique
        );

        ContentSigner signer = new JcaContentSignerBuilder(KEY_ALGORITHM).build(privateKey);

        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

}
