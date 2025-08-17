package fr.amynna.OriginLauncher.tools;

/**
 * Classe {@code Printer} fournit des méthodes pour afficher des messages dans la console
 * avec différents niveaux de gravité (info, erreur, avertissement, débogage).
 * Elle utilise des codes ANSI pour formater le texte en couleur.
 */
public final class Printer {

    public static final String BOLD = "\033[1m";
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String WHITE = "\033[37m";
    public static final String BLUE = "\033[34m";

    /**
     * Constructeur privé pour empêcher l'instanciation de cette classe utilitaire.
     */
    public static void println(String message) {
        System.out.println(message);
    }

    /**
     * Affiche un message d'information dans la console.
     *
     * @param message Le message à afficher
     */
    public static void info(String message) {
        println(BOLD + WHITE+  "[ " + GREEN + "INFO" + WHITE + " ] " + RESET + message);
    }

    /**
     * Affiche un message d'erreur dans la console.
     *
     * @param message Le message d'erreur à afficher
     */
    public static void error(String message) {
        println(BOLD + WHITE + "[ " + RED + "ERROR" + WHITE + " ] " + RESET + message);
    }

    /**
     * Affiche un message d'avertissement dans la console.
     *
     * @param message Le message d'avertissement à afficher
     */
    public static void warning(String message) {
        println(BOLD + WHITE + "[ " + YELLOW + "WARN" + WHITE + " ] " + RESET + message);
    }

    /**
     * Affiche un message de débogage dans la console.
     *
     * @param message Le message de débogage à afficher
     */
    public static void printDebug(String message) {
        println(BOLD + YELLOW + "[ " + BLUE + "DEBUG" + YELLOW + " ] " + RESET + YELLOW + message);
    }

    /**
     * Affiche un message de fatal error dans la console et termine le programme.
     *
     * @param message Le message de fatal error à afficher
     */
    public static void fatalError(String message) {
        println(BOLD + RED + "[ FATL ERROR ] " + RESET + RED + message);
        System.exit(0);
    }


}
