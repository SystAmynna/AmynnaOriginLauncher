package com.amynna.OriginBootstrap;

import com.amynna.Tools.*;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Point d'entrée du BootStrap de l'application.
 */
public final class App {

    /**
     * Lance le processus de lancement de l'application.
     */
    private void launch() {
        new Launch().process();
    }

    /**
     * Génère une paire de clés publique/privée.
     */
    private void genKeys(String ... args) {

        if (args.length == 2) {
            KeyUtil.generatePrivateKey(args[1]);
        }

    }

    /**
     * Signe un fichier avec une clé privée.
     */
    private void sign(String ... args) {

        if (args.length != 3) {
            Logger.log("Please provide the file path to sign and the alias of the private key.");
            return;
        }

        String filePath = args[1];
        String keyAlias = args[2];
        String password = Asker.askPassword();
        PrivateKey privateKey = KeyUtil.loadPrivateKey(keyAlias, password);

        KeyUtil.signFile(filePath, privateKey);
    }

    /**
     * Vérifie la signature d'un fichier avec une clé publique.
     */
    private void verify(String ... args) {

        SignedFile signedFile;
        if (args.length == 3) {
            signedFile = new SignedFile(new File(args[1]), new File(args[2]));
            KeyUtil.validateSignature(signedFile);
        } else Logger.log("Please provide the signed file path and the public key path.");
    }

    /**
     * Affiche l'aide avec les commandes disponibles.
     */
    private void help() {
        Logger.log("Available commands: launch, genKeys, sign, verify, help");
    }

    /**
     * Affiche le contenu d'une clé (publique ou privée) à partir d'un fichier.
     */
    private void showKey(String ... args) {
        if (args.length != 2) {
            Logger.log("Please provide the alias of the key to display.");
            return;
        }

        String keyAlias = args[1];
        if (keyAlias.isEmpty()) {
            Logger.log("Key alias cannot be empty.");
            return;
        }

        PrivateKey privateKey = KeyUtil.loadPrivateKey(keyAlias, Asker.askPassword());
        PublicKey publicKey = KeyUtil.getPublicKeyFromPrivateKey(privateKey);

        assert publicKey != null;
        String publicKeyString = KeyUtil.getPublicKeyAsString(publicKey);

        Logger.log("Public Key:\n" + publicKeyString);

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
            default -> Logger.log("Commande inconnue. Utilisez 'help' pour voir les commandes disponibles.");
        }

    }



}
