package com.amynna.OriginBootstrap;

import com.amynna.OriginBootstrap.tasks.Launch;

import java.io.File;

/**
 * Point d'entrée du BootStrap de l'application.
 */
public final class App {

    /**
     * URL du serveur distant pour les mises à jour et la récupération des clés publiques.
     */
    public final String SERVER_URL = "http://localhost:8080";
    /**
     * Nom de l'application.
     */
    public final String APP_NAME = "OriginRP";
    /**
     * Répertoire racine du lanceur.
     */
    public final String LAUNCHER_ROOT = System.getProperty("user.home") + File.separator + "." + APP_NAME + File.separator;


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

        if (args.length != 3) {
            System.out.println("Please provide the signed file path and the public key path.");
            return;
        }

        File signedFile = new File(args[1]);
        String publicKeyPath = args[2];

        System.out.println(KeyUtil.verifySignature(signedFile, publicKeyPath));

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
            System.out.println("Please provide the key file path.");
            return;
        }
        String keyFilePath = args[1];
        System.out.println(keyFilePath + " :\n" + KeyUtil.keyAsString(keyFilePath));
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
