package old.old.Tools;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.security.*;
import java.util.Date;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * Utilitaire pour la gestion des clés cryptographiques et des signatures.
 */
public final class KeyUtil {

    // ---- [ ATTRIBUTS ] ----

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
    public static final String KEY_STORE_TYPE = "PKCS12";

    /**
     * Indicateur si le gestionnaire de clés a été initialisé.
     */
    private static boolean initialized = false;

    // ---- [ INITIALISATION ] ----

    /**
     * Initialise le gestionnaire de clés avec l'application donnée.
     */
    private static void init() {
        // Vérifier si déjà initialisé
        if (initialized) return;
        initialized = true;

        // Ajouter la clé publique de confiance prioritaire (Master Key)
        TRUSTED_PUBLIC_KEYS.put(AppProperties.DEFAULT_PUBLIC_KEY_OWNER, getPublicKeyFromString(AppProperties.DEFAULT_PUBLIC_KEY));

        Logger.log("🔐 Clé publique de confiance prioritaire (Master Key) : " + Logger.BOLD +
                AppProperties.DEFAULT_PUBLIC_KEY_OWNER);

        // Télécharger le fichier des clés publiques de confiance
        final String trustedKeysFileName = "trusted-keys.json";
        SignedFile trustedKeysFile = FileManager.downloadAndValidateFile(trustedKeysFileName, AppProperties.TEMP_DIR.toPath() + File.separator + trustedKeysFileName);
        if (trustedKeysFile == null) {
            Logger.error("⚠️  Impossible de charger le fichier des clés publiques de confiance.");
            return;
        }

        // Lire le fichier des clés publiques de confiance et extraire les clés publiques
        final JSONObject trustedKeysJson = FileManager.openJsonFile(trustedKeysFile.file());
        if (trustedKeysJson == null || !trustedKeysJson.has("trusted_keys")) {
            Logger.error("⚠️  Le fichier des clés publiques de confiance est invalide.");
            return;
        }
        JSONArray trustedArray = trustedKeysJson.getJSONArray("trusted_keys");
        if (trustedArray.isEmpty()) {
            Logger.error("⚠️  Le fichier des clés publiques de confiance est vide ou invalide.");
            return;
        }
        for (int i = 0; i < trustedArray.length(); i++) {
            JSONObject entry = trustedArray.getJSONObject(i);
            String keyName = entry.getString("name");
            String keyValue = entry.getString("key");
            TRUSTED_PUBLIC_KEYS.put(keyName, getPublicKeyFromString(keyValue));
        }

        // Lister les clés publiques de confiance chargées
        StringBuilder keysList = new StringBuilder();
        for (Map.Entry<String, PublicKey> entry : TRUSTED_PUBLIC_KEYS.entrySet()) {
            if (entry.getKey().equals(AppProperties.DEFAULT_PUBLIC_KEY_OWNER)) continue;
            keysList.append(entry.getKey()).append("  ");
        }
        Logger.log("🔑 Clés publiques de confiance (Certifiées par la Master Key) : " + Logger.BOLD + keysList + Logger.RESET);
    }

    // ---- [ MÉTHODES PUBLIQUES ] ----

    /**
     * Valide la signature d'un fichier en utilisant les clés publiques de confiance.
     * @param signedFile Le fichier à valider.
     * @return true si la signature est valide avec au moins une clé publique de confiance, false sinon.
     */
    public static boolean validateSignature(SignedFile signedFile) {
        return validateSignature(signedFile, "");
    }

    private static boolean validateSignature(SignedFile signedFile, String space) {
        // Initialiser le gestionnaire de clés si nécessaire
        init();

        //Logger.log("DEBUG: " + signedFile.file().getName() + " --- " + signedFile.signature().getName());

        if (signedFile.isDirectory()) {

            File[] subFiles = signedFile.file().listFiles();
            File[] subSignFiles = signedFile.signature().listFiles();
            if (subFiles == null || subSignFiles == null) {
                Logger.error("Impossible de lister les fichiers dans le répertoire signé.");
                return false;
            }


            if (subFiles.length == 0 && subSignFiles.length == 0) {
                Logger.log(space + "⚠️  Répertoire [" + signedFile.file().getName() +
                        "] et son répertoire de signatures sont vides. Rien à valider.");
                return true;
            }

            if (subFiles.length != subSignFiles.length) {
                Logger.error("Le nombre de fichiers dans [" + signedFile.file().getName() +
                        "] et de signatures dans ["+ signedFile.signature().getName() + "] ne correspond pas.");
                return false;
            }

            // Associer chaque sous-fichier avec son fichier de signature
            SignedFile[] signedSubFiles = new SignedFile[subFiles.length];
            int index = 0;
            for (File file : subFiles) {
                for (File signFile : subSignFiles) {
                    if (signFile.getName().equals(file.getName()) || signFile.getName().equals(file.getName() + AppProperties.SIGNATURE_FILE_EXTENSION)) { // + AppProperties.SIGNATURE_FILE_EXTENSION
                        signedSubFiles[index] = new SignedFile(file, signFile);
                        index++;
                        break;
                    }
                }
            }

            // Vérifier chaque sous-fichier
            boolean valid = true;
            for (SignedFile sf : signedSubFiles) {
                boolean subValid = validateSignature(sf, space + "   ");
                if (valid) valid = subValid;
            }
            if (valid) {
                Logger.log(space + "✅ Répertoire [" + signedFile.file().getName() +
                        "] signé par [" + signedFile.signature().getName() +
                        "] validé avec les clés publiques de confiance.");
                return true;
            }
            Logger.log(space + "❌ Répertoire [" + signedFile.file().getName() +
                    "] non signé par [" + signedFile.signature().getName() +
                    "] avec les clés publiques de confiance.");
            return false;

        }
        else {
            // Vérifier la signature avec chaque clé publique de confiance
            for (String iPublicKey : TRUSTED_PUBLIC_KEYS.keySet()) {
                if (verifyFile(signedFile, TRUSTED_PUBLIC_KEYS.get(iPublicKey))) {
                    Logger.log(space + "✅ Fichier [" + signedFile.file().getName() +
                            "] signé par [" + signedFile.signature().getName() +
                            "] validé avec la clé publique de confiance : " + iPublicKey);
                    return true;
                }
            }
            Logger.log(space + "❌ Fichier [" + signedFile.file().getName() +
                    "] non signé par [" + signedFile.signature().getName() +
                    "] avec aucune clé publique de confiance.");
            return false;
        }

    }



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
            Logger.error("Erreur : Le chemin spécifié n'est ni un fichier ni un répertoire valide.");
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
     * Génère une paire de clés publique/privée et sauvegarde la clé privée dans le KeyStore.
     */
    public static void generateKeys(String alias) {
        try {

            if (alias == null || alias.trim().isEmpty()) {
                Logger.error("❌ Alias requis pour générer une clé.");
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
            Logger.log("🔑 Clé publique générée (à partager) :");
            Logger.log(getPublicKeyAsString(publicKey));


        } catch (Exception e) {
            Logger.error("❌ Erreur lors de la génération de la clé : " + e.getMessage());
            return;
        }
    }

    /**
     * Supprime une paire de clés (privée + publique) du KeyStore.
     * @param alias L'alias de la clé à supprimer.
     * @param password Le mot de passe pour accéder au KeyStore.
     * @return true si la suppression a réussi, false sinon.
     */
    public static void deleteKeys(String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;

            if (!ksFile.exists()) {
                Logger.error("❌ KeyStore introuvable.");
                return;
            }

            // Charger le KeyStore
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Vérifier que l'alias existe
            if (!keyStore.containsAlias(alias)) {
                Logger.error("❌ Aucune clé trouvée pour l'alias : " + alias);
                return;
            }

            if (!Asker.confirmAction("Confirmer la suppression de la clé '" + alias + "' ?")) {
                Logger.log("Suppression annulée par l'utilisateur.");
                return;
            }

            // Supprimer l'entrée
            keyStore.deleteEntry(alias);

            // Sauvegarder le KeyStore modifié
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                keyStore.store(fos, password.toCharArray());
            }

            Logger.log("🗑️  Clé supprimée : " + alias);

        } catch (Exception e) {
            Logger.error("❌ Erreur lors de la suppression : " + e.getMessage());
        }
    }

    /**
     * Change le mot de passe d'accès global du KeyStore.
     * @param oldPassword L'ancien mot de passe du KeyStore.
     * @return true si le changement a réussi, false sinon.
     */
    public static void changeKeyStorePassword(String oldPassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            File ksFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;

            if (!ksFile.exists()) {
                Logger.error("❌ KeyStore introuvable.");
                return;
            }

            // Charger le KeyStore avec l'ancien mot de passe
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, oldPassword.toCharArray());
            }

            // Demander le nouveau mot de passe
            String newPassword = Asker.askFirstPassword();

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

            Logger.log("🔐 Mot de passe du KeyStore changé avec succès");

        } catch (Exception e) {
            Logger.error("❌ Erreur lors du changement de mot de passe : " + e.getMessage());
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
                Logger.error("❌ KeyStore introuvable.");
                return null;
            }

            // Charger le KeyStore
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                keyStore.load(fis, password.toCharArray());
            }

            // Récupérer tous les alias
            java.util.Enumeration<String> aliases = keyStore.aliases();

            if (!aliases.hasMoreElements()) {
                Logger.log("ℹ️  Aucune clé stockée dans le KeyStore.");
                return keys;
            }

            Logger.log("🔑 Clés stockées dans le KeyStore :");
            Logger.log("═══════════════════════════════════");

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                keys.add(alias);

                if (keyStore.isKeyEntry(alias)) {
                    // C'est une clé privée
                    Certificate cert = keyStore.getCertificate(alias);
                    PublicKey publicKey = cert.getPublicKey();

                    printKeyInfo(alias, publicKey);
                    Logger.log("───────────────────────────────────");

                } else if (keyStore.isCertificateEntry(alias)) {
                    // C'est uniquement un certificat
                    Certificate cert = keyStore.getCertificate(alias);
                    PublicKey publicKey = cert.getPublicKey();

                    Logger.log("📌 Alias : " + Logger.BOLD + alias + Logger.RESET);
                    Logger.log("   Type : Certificat seul");
                    Logger.log("   Algorithme : " + publicKey.getAlgorithm());
                    Logger.log("───────────────────────────────────");
                }
            }

            return keys;

        } catch (Exception e) {
            Logger.error("❌ Erreur lors de la liste des clés : " + e.getMessage());
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
                Logger.error("Aucun certificat trouvé pour l'alias : " + alias);
                return null;
            }

            return cert.getPublicKey();

        } catch (Exception e) {
            Logger.error("Erreur lors du chargement de la clé publique : " + e.getMessage());
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
            Logger.error("Clé publique invalide (null)");
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
            Logger.error("Erreur lors de la conversion de la clé publique : " + e.getMessage());
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
        Logger.log("📌 Alias : " + Logger.BOLD + alias);
        Logger.log("   Clé publique : " + getPublicKeyAsString(publicKey));
    }

    // ---- [ MÉTHODES PRIVÉES ] ----

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
                Logger.error("❌ Alias déjà utilisé. Choisissez un autre alias.");
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

            Logger.log("🔒 Clés privée et publique sauvegardées, alias : " + alias);

        } catch (Exception e) {
            Logger.error("Erreur lors de la sauvegarde : " + e.getMessage());
        }
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
            Logger.error("Erreur : Le fichier à signer est introuvable.");
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
            Signature sig = Signature.getInstance(KEY_ALGORITHM);
            sig.initSign(privateKey);
            sig.update(data);
            byte[] sigBytes = sig.sign();

            // Sauvegarder la signature dans un fichier (encodée en Base64)
            Files.write(signFile.toPath(), sigBytes);

            Logger.log("Signature générée : " + signFilePath);
            return new SignedFile(file, signFile);
        } catch (Exception e) {
            Logger.error("Erreur lors de la signature du fichier : " + e.getMessage());
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
            Logger.error("Le répertoire à signer est introuvable.");
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
            Logger.error("Impossible de lister les fichiers dans le répertoire à signer.");
            return null;
        }
        if (files.length == 0) {
            Logger.log("Le répertoire à signer est vide, ignoré.");
            return new SignedFile(dir, signDirectory);
        }

        // Signer chaque fichier
        for (File file : files) {
            if (file.isFile()) signFile(file, signDirectoryPath, privateKey);
            else if (file.isDirectory()) signDirectory(file, signDirectoryPath, privateKey);
            else Logger.log("Ignoré (fichier ou repertoire invalide) : " + file.getName());
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

    /**
     * Vérifie la signature d'un fichier avec une clé publique donnée.
     * @param signedFile Le fichier signé.
     * @param publicKey La clé publique.
     * @return true si la signature est valide, false sinon.
     */
    public static boolean verifyFile(SignedFile signedFile, PublicKey publicKey) {

        try {
            // data du fichier
            byte[] data = Encrypter.getFileBytesNormalized(signedFile.file());
            // data de la signature
            byte[] sigBytes = Encrypter.getFileBytesNormalized(signedFile.signature());

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
     * Crée un certificat X.509 contenant la clé publique fournie.
     * @param privateKey La clé privée pour signer le certificat.
     * @param publicKey La clé publique à inclure dans le certificat.
     * @return Le certificat X.509 auto-signé.
     * @throws Exception En cas d'erreur lors de la création du certificat.
     */
    private static Certificate createCertificateWithPublicKey(PrivateKey privateKey, PublicKey publicKey) throws Exception {
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
