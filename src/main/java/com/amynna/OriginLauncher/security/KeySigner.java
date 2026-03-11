package com.amynna.OriginLauncher.security;

import com.amynna.OriginLauncher.output.Logger;

import java.io.File;
import java.security.PrivateKey;

public class KeySigner {

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


}
