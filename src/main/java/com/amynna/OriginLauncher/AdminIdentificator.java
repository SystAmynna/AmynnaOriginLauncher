package com.amynna.OriginLauncher;

import com.amynna.Tools.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.util.Date;
import java.util.List;

public class AdminIdentificator {

    public static boolean admin = false;

    private static final String ERROR_ADMIN_NOT_IDENTIFIED = "Échec de l'identification de l'administrateur";


    public static boolean isAdmin() {
        return admin;
    }

    protected static void checkAdmin() {

        // Récupère la liste des clées locales
        String pwd = Asker.askPassword();
        List<String> localKeys = KeyUtil.listKeys(pwd);
        if (localKeys == null || localKeys.isEmpty()) {
            Logger.error(ERROR_ADMIN_NOT_IDENTIFIED + " : aucune clé locale trouvée.");
            return;
        }

        // Créer un répertoire temporaire pour vérifier les clés
        File adminCheckDir = new File(AppProperties.TEMP_DIR.toPath() + File.separator + "admin_check");
        FileManager.createDirectoriesIfNotExist(adminCheckDir.getPath());

        // Créer un fichier temporaire à signer
        File testFile = new File(adminCheckDir.getPath() + File.separator + "admin_check_file.txt");
        try {
            if (!testFile.createNewFile()) throw new IOException();
            Date now = new Date();
            final String testContent = Encrypter.sha512(now.toString());
            Files.writeString(testFile.toPath(), testContent);
        } catch (IOException | SecurityException e) {
            Logger.error(ERROR_ADMIN_NOT_IDENTIFIED + " : échec de la création du fichier de test.");
            return;
        }

        // Signer ce fichier avec chaque clé locale
        for (String keyAlias : localKeys) {
            // Charger la clé privée
            PrivateKey privateKey = KeyUtil.loadPrivateKey(keyAlias, pwd);
            // Signer le fichier de test
            File signatureFile = KeyUtil.signFile(testFile.getPath(), privateKey);
            // Vérifier la signature
            if (signatureFile == null) continue;
            // Créer l'objet SignedFile pour la vérification
            SignedFile signedFile = new SignedFile(testFile, signatureFile);
            // Vérifier la validité de la signature
            if (signedFile.valid()) {
                admin = true;
                Logger.log(Logger.PURPLE + Logger.BOLD + "Identification en tant qu'administrateur réussie !");
                return;
            }

        }
        Logger.error(ERROR_ADMIN_NOT_IDENTIFIED + " : aucune clé valide trouvée.");

    }








}
