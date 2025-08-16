package fr.amynna.OriginLauncher.tools;

public final class Printer {

    public static void println(String message) {
        System.out.println(message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void printError(String message) {
        System.err.println("[ERROR] " + message);
    }

    public static void printWarning(String message) {
        System.out.println("[WARN] " + message);
    }

    public static void printDebug(String message) {
        System.out.println("[DEBUG] " + message);
    }

    public static void fatalError(String message) {
        System.out.println("[FATAL ERROR] " + message);
        System.exit(0);
    }


}
