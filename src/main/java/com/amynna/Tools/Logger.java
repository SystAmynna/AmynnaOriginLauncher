package com.amynna.Tools;

import java.util.LinkedList;
import java.util.List;

/**
 * Classe de journalisation pour afficher des messages colorés dans la console.
 */
public final class Logger {

    /** Liste des messages de log */
    private static final List<String> logMessages = new LinkedList<String>();

    /** Codes ANSI pour RESET le formatage */
    public static final String RESET = "\u001B[0m";
    /** Code ANSI pour le texte en BOLD */
    public static final String BOLD = "\u001B[1m";
    /** Code ANSI pour le texte en UNDERLINE */
    public static final String UNDERLINE = "\u001B[4m";

    /** Codes ANSI pour la couleur BLEU */
    public static final String BLUE = "\u001B[34m";
    /** Codes ANSI pour la couleur VERTE */
    public static final String GREEN = "\u001B[32m";
    /** Codes ANSI pour la couleur ORANGE */
    public static final String ORANGE = "\u001B[33m";
    /** Codes ANSI pour la couleur ROUGE */
    public static final String RED = "\u001B[31m";
    /** Codes ANSI pour la couleur POURPRE */
    public static final String PURPLE = "\u001B[35m";

    /** Log un message sans saut de ligne */
    public static void logc(String message) {
        message = message + RESET;
        System.out.print(message);
    }

    /** Log un message avec saut de ligne */
    public static void log(String message) {
        message = message + RESET;
        System.out.println(message);
        logMessages.add(message );
    }

    /** Log une erreur */
    public static void error(String message) {
        log(RED + BOLD + "[ERROR]: " + RESET + RED + message);
    }

    /** Log un message fatal et quitte l'application */
    public static void fatal(String message) {
        error(message);
        saveLogToFile();
        System.exit(1);
    }

    /** Affiche la version de l'application */
    public static void version() {
        log(AppProperties.APP_NAME + " version " + AppProperties.APP_VERSION + " \nby " + AppProperties.APP_AUTHOR);
    }

    /** Sauvegarde les messages de log dans un fichier */
    private static void saveLogToFile() {


    }

    /** Récupère tous les messages de log sous forme de chaîne */
    public static String getLogMessages() {
        StringBuilder sb = new StringBuilder();
        for (String msg : logMessages) {
            sb.append(msg).append(System.lineSeparator());
        }
        return sb.toString();
    }


}
