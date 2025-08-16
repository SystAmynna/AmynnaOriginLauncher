package fr.amynna.OriginLauncher.tools;

public final class Printer {

    public static final String BOLD = "\033[1m";
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String WHITE = "\033[37m";
    public static final String BLUE = "\033[34m";

    public static void println(String message) {
        System.out.println(message);
    }

    public static void printInfo(String message) {
        println(BOLD + WHITE+  "[ " + GREEN + "INFO" + WHITE + " ] " + RESET + message);
    }

    public static void printError(String message) {
        println(BOLD + WHITE + "[ " + RED + "ERROR" + WHITE + " ] " + RESET + message);
    }

    public static void printWarning(String message) {
        println(BOLD + WHITE + "[ " + YELLOW + "WARN" + WHITE + " ] " + RESET + message);
    }

    public static void printDebug(String message) {
        println(BOLD + WHITE + "[ " + BLUE + "DEBUG" + WHITE + " ] " + RESET + message);
    }

    public static void fatalError(String message) {
        println(BOLD + RED + "[ FATL ERROR ] " + RESET + RED + message);
        System.exit(0);
    }


}
