package com.amynna.OriginBootstrap;

import com.amynna.Tools.KeyUtil;
import com.amynna.Tools.Logger;
import com.amynna.Tools.SignedFile;

import java.io.File;

/**
 * Point d'entrée du BootStrap de l'application.
 */
public final class App {

    /**
     * Lance le processus de lancement de l'application.
     */
    private void launch() {
        KeyUtil.init(this);
        new Launch(this).process();
    }

    /**
     * Génère une paire de clés publique/privée.
     */
    private void genKeys() {
        KeyUtil.generateKeys();
    }

    /**
     * Signe un fichier avec une clé privée.
     */
    private void sign(String ... args) {

        if (args.length != 3) {
            System.out.println("Please provide the file path to sign and the private key path.");
            return;
        }

        String filePath = args[1];
        String privateKeyPath = args[2];

        KeyUtil.signFile(filePath, privateKeyPath);
    }

    /**
     * Vérifie la signature d'un fichier avec une clé publique.
     */
    private void verify(String ... args) {

        SignedFile signedFile;
        if (args.length == 4) {
            signedFile = new SignedFile(new File(args[1]), new File(args[2]));
            File publicKey = new File(args[3]);
            String keyString = KeyUtil.loadKeyAsString(publicKey);
            boolean valid = KeyUtil.verifyFile(signedFile, keyString);
            Logger.log(valid ? "✅ Signature is valid." : "❌ Signature is NOT valid.");
        } else if (args.length == 3) {
            signedFile = new SignedFile(new File(args[1]), new File(args[2]));
            KeyUtil.init(this);
            KeyUtil.validateSignature(signedFile);
        } else Logger.log("Please provide the signed file path and the public key path.");
    }

    /**
     * Affiche l'aide avec les commandes disponibles.
     */
    private void help() {
        System.out.println("Available commands: launch, genKeys, sign, verify, help");
    }

    /**
     * Affiche le contenu d'une clé (publique ou privée) à partir d'un fichier.
     */
    private void showKey(String ... args) {
        if (args.length != 2) {
            Logger.log("Please provide the key file path.");
            return;
        }
        String keyFilePath = args[1];
        Logger.log(keyFilePath + " :\n" + KeyUtil.loadKeyAsString(new File(keyFilePath)));
    }

    /**
     * Point d'entrée principal de l'application.
     * @param args Arguments de la ligne de commande.
     */
    public static void main(String[] args) {

        App app = new App();

        if (args.length == 0) app.launch();

        switch (args[0]) {
            case "launch" -> app.launch();
            case "genKeys" -> app.genKeys();
            case "showKey" -> app.showKey(args);
            case "sign" -> app.sign(args);
            case "verify" -> app.verify(args);
            case "help" -> app.help();
            default -> System.out.println("Commande inconnue. Utilisez 'help' pour voir les commandes disponibles.");
        }

    }



}
