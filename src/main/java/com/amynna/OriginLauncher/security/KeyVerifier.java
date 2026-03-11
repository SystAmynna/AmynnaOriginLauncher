package com.amynna.OriginLauncher.security;

import com.amynna.OriginLauncher.AppProperties;
import com.amynna.OriginLauncher.output.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.security.PublicKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

public class KeyVerifier {

    /**
     * Liste des clés publiques de confiance pour valider les signatures des fichiers.
     * Sous format: Alias - clé publique
     */
    private static final Map<String, PublicKey> TRUSTED_PUBLIC_KEYS = new HashMap<>();

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
        initialized = true;

        // Ajouter la clé publique de confiance prioritaire (Master Key)
        TRUSTED_PUBLIC_KEYS.put(AppProperties.DEFAULT_PUBLIC_KEY_OWNER, KeyTool.getPublicKeyFromString(AppProperties.DEFAULT_PUBLIC_KEY));

        Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔐 Clé publique de confiance prioritaire (Master Key) : " + Logger.BOLD +
                AppProperties.DEFAULT_PUBLIC_KEY_OWNER);

        // Télécharger le fichier des clés publiques de confiance
        final String trustedKeysFileName = "trusted-keys.json";
        SignedFile trustedKeysFile = FileManager.downloadAndValidateFile(trustedKeysFileName, AppProperties.TEMP_DIR.toPath() + File.separator + trustedKeysFileName);
        if (trustedKeysFile == null) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"⚠️  Impossible de charger le fichier des clés publiques de confiance.");
            return;
        }

        // Lire le fichier des clés publiques de confiance et extraire les clés publiques
        final JSONObject trustedKeysJson = FileManager.openJsonFile(trustedKeysFile.file());
        if (trustedKeysJson == null || !trustedKeysJson.has("trusted_keys")) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"⚠️  Le fichier des clés publiques de confiance est invalide.");
            return;
        }
        JSONArray trustedArray = trustedKeysJson.getJSONArray("trusted_keys");
        if (trustedArray.isEmpty()) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"⚠️  Le fichier des clés publiques de confiance est vide ou invalide.");
            return;
        }
        for (int i = 0; i < trustedArray.length(); i++) {
            JSONObject entry = trustedArray.getJSONObject(i);
            String keyName = entry.getString("name");
            String keyValue = entry.getString("key");
            TRUSTED_PUBLIC_KEYS.put(keyName, KeyTool.getPublicKeyFromString(keyValue));
        }

        // Lister les clés publiques de confiance chargées
        StringBuilder keysList = new StringBuilder();
        for (Map.Entry<String, PublicKey> entry : TRUSTED_PUBLIC_KEYS.entrySet()) {
            if (entry.getKey().equals(AppProperties.DEFAULT_PUBLIC_KEY_OWNER)) continue;
            keysList.append(entry.getKey()).append("  ");
        }
        Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"🔑 Clés publiques de confiance (Certifiées par la Master Key) : " + Logger.BOLD + keysList + Logger.RESET);
    }



    /**
     * Valide la signature d'un fichier en utilisant les clés publiques de confiance.
     * @param signedFile Le fichier à valider.
     * @return true si la signature est valide avec au moins une clé publique de confiance, false sinon.
     */
    public static boolean validateSignature(SignedFile signedFile) {
        return validateSignature(signedFile, "");
    }

    /** Valide la signature d'un fichier en utilisant les clés publiques de confiance.
     * @param signedFile Le fichier à vérifier
     * @param space L'espacement dans les logs
     * @return true si la signature est valide avec au moins une clée publique de confiance, false sinon.
     */
    private static boolean validateSignature(SignedFile signedFile, String space) {
        // Initialiser le gestionnaire de clés si nécessaire
        init();


        if (signedFile.isDirectory()) {

            File[] subFiles = signedFile.file().listFiles();
            File[] subSignFiles = signedFile.signature().listFiles();
            if (subFiles == null || subSignFiles == null) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Impossible de lister les fichiers dans le répertoire signé.");
                return false;
            }


            if (subFiles.length == 0 && subSignFiles.length == 0) {
                Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,space + "⚠️  Répertoire [" + signedFile.file().getName() +
                        "] et son répertoire de signatures sont vides. Rien à valider.");
                return true;
            }

            if (subFiles.length != subSignFiles.length) {
                Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,"Le nombre de fichiers dans [" + signedFile.file().getName() +
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
                Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,space + "✅ Répertoire [" + signedFile.file().getName() +
                        "] signé par [" + signedFile.signature().getName() +
                        "] validé avec les clés publiques de confiance.");
                return true;
            }
            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,space + "❌ Répertoire [" + signedFile.file().getName() +
                    "] non signé par [" + signedFile.signature().getName() +
                    "] avec les clés publiques de confiance.");
            return false;

        }
        else {
            // Vérifier la signature avec chaque clé publique de confiance
            for (String iPublicKey : TRUSTED_PUBLIC_KEYS.keySet()) {
                if (verifyFile(signedFile, TRUSTED_PUBLIC_KEYS.get(iPublicKey))) {
                    Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,space + "✅ Fichier [" + signedFile.file().getName() +
                            "] signé par [" + signedFile.signature().getName() +
                            "] validé avec la clé publique de confiance : " + iPublicKey);
                    return true;
                }
            }
            Logger.log(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD,space + "❌ Fichier [" + signedFile.file().getName() +
                    "] non signé par [" + signedFile.signature().getName() +
                    "] avec aucune clé publique de confiance.");
            return false;
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
            byte[] data = Encrypter.getFileBytesNormalized(signedFile.file());
            // data de la signature
            byte[] sigBytes = Encrypter.getFileBytesNormalized(signedFile.signature());

            Signature sig = Signature.getInstance(KeyTool.KEY_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(data);

            return sig.verify(sigBytes);
        } catch (Exception e) {
            Logger.error(Logger.LOG_CANAL.KEYS, Logger.VERBOSE_LEVEL.STANDARD, e,"Erreur lors de la vérification de la signature");
            return false;
        }
    }




}
