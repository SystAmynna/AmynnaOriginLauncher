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
     * @param args Arguments de la ligne de commande.
     */
    private void genKeys(String ... args) {
        if (args.length == 2) {
            String alias = args[1];
            KeyUtil.generateKeys(alias);
        } else {
            Logger.log("L'alias de la clé est requis. Usage: genKeys <alias>");
        }
    }

    /**
     * Signe un fichier avec une clé privée.
     * @param args Arguments de la ligne de commande.
     */
    private void sign(String ... args) {
        if (args.length != 3) {
            Logger.log("Le chemin du fichier et l'alias de la clé sont requis. Usage: sign <filePath> <keyAlias>");
            return;
        }

        String filePath = args[1];
        String keyAlias = args[2];
        String password = Asker.askPassword();
        PrivateKey privateKey = KeyUtil.loadPrivateKey(keyAlias, password);

        File file = new File(filePath);

        KeyUtil.sign(file, privateKey);
    }

    /**
     * Vérifie la signature d'un fichier avec une clé publique.
     * @param args Arguments de la ligne de commande.
     */
    private void verify(String ... args) {
        SignedFile signedFile;
        if (args.length == 3) {
            signedFile = new SignedFile(new File(args[1]), new File(args[2]));
            signedFile.valid();
        } else Logger.log("Le chemin du fichier et le chemin de la signature sont requis. Usage: verify <filePath> <signaturePath>");
    }

    /**
     * Affiche la clé publique associée à un alias donné.
     * @param args Arguments de la ligne de commande.
     */
    private void showKey(String ... args) {
        if (args.length != 2) {
            Logger.log("L'alias de la clé est requis. Usage: showKey <keyAlias>");
            return;
        }

        String keyAlias = args[1];
        String password = Asker.askPassword();
        PublicKey publicKey = KeyUtil.loadPublicKey(keyAlias, password);

        KeyUtil.printKeyInfo(keyAlias, publicKey);
    }

    /**
     * Supprime une paire de clés associée à un alias donné.
     * @param args Arguments de la ligne de commande.
     */
    private void delKey(String ... args) {
        if (args.length != 2) {
            Logger.log("L'alias de la clé est requis. Usage: delKey <keyAlias>");
            return;
        }

        String keyAlias = args[1];
        String password = Asker.askPassword();

        KeyUtil.deleteKeys(keyAlias, password);
    }

    /**
     * Liste toutes les clés stockées dans le keystore.
     */
    private void listKeys() {
        String password = Asker.askPassword();
        KeyUtil.listKeys(password);
    }

    /**
     * Change le mot de passe du keystore.
     */
    private void changePassword() {
        String password = Asker.askPassword();
        KeyUtil.changeKeyStorePassword(password);
    }

    private void version() {
        Logger.version();
    }

    /**
     * Affiche l'aide avec les commandes disponibles.
     */
    private void help() {
        Logger.log("Available commands:\n" +
                "  launch                               : Lance l'application.\n" +
                "  genKeys <alias>                      : Génère une paire de clés publique/privée avec l'alias spécifié.\n" +
                "  showKey <keyAlias>                   : Affiche la clé publique associée à l'alias donné.\n" +
                "  sign <filePath> <keyAlias>           : Signe le fichier spécifié avec la clé privée associée à l'alias donné.\n" +
                "  verify <filePath> <signaturePath>    : Vérifie la signature du fichier avec la signature fournie.\n" +
                "  delKey <keyAlias>                    : Supprime la paire de clés associée à l'alias donné.\n" +
                "  listKeys                             : Liste toutes les clés stockées dans le keystore.\n" +
                "  changePassword                       : Change le mot de passe du keystore.\n" +
                "  help                                 : Affiche cette aide."

        );
    }


    /**
     * Point d'entrée principal de l'application.
     * @param args Arguments de la ligne de commande.
     */
    public static void main(String[] args) {

        if (!FileManager.pingServer(AppProperties.REPO_SERVER_URL)) {
            Logger.log("Le serveur n'est pas accessible. Veuillez vérifier votre connexion internet.");
            return;
        }

        App app = new App(); // Instance de l'application pour accéder aux méthodes non statiques

        FileManager.createDirectoriesIfNotExist(AppProperties.LAUNCHER_ROOT.getPath()); // Assure que le répertoire racine existe

        FileManager.deleteFileIfExists(AppProperties.TEMP_DIR); // Nettoie le répertoire temporaire

        if (args.length == 0) {
            app.launch();
            return;
        }

        switch (args[0]) {
            case "launch" -> app.launch();
            case "genKeys", "gen" -> app.genKeys(args);
            case "showKey" -> app.showKey(args);
            case "sign" -> app.sign(args);
            case "verify" -> app.verify(args);
            case "delKey", "del" -> app.delKey(args);
            case "listKeys", "list", "ls" -> app.listKeys();
            case "changePassword", "passwd", "pass" -> app.changePassword();
            case "help" -> app.help();
            case "version" -> app.version();
            default -> Logger.log("Commande inconnue. Utilisez 'help' pour voir les commandes disponibles.");
        }

    }



}
