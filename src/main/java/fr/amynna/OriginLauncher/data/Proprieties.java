package fr.amynna.OriginLauncher.data;

/**
 * Classe {@code Proprieties} qui contient les propriétés de l'application.
 */
public class Proprieties {

    /**
     * Nom de l'application.
     */
    public static final String NAME = "OriginRP";
    /**
     * Version de l'application.
     */
    public static final String VERSION = "0.1.0";
    /**
     * Auteur de l'application.
     */
    public static final String AUTHOR = "SystAmynna";
    /**
     * Version de minecraft supportée par l'application.
     */
    public static final String MINECRAFT_VERSION = "1.20.1";

    /**
     * Chemin vers la racine de l'application.
     */
    public static final String ROOT_PATH = System.getProperty("user.home") + "/." + NAME;
    /**
     * Chemin vers le .minecraft de l'application.
     */
    public static final String MC_PATH = ROOT_PATH + "/.minecraft";

    /**
     * Enumération représentant les systèmes d'exploitation supportés par l'application.
     */
    public static enum OS {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }
    /**
     * Méthode pour obtenir le système d'exploitation actuel.
     *
     * @return Le système d'exploitation actuel.
     */
    public static OS getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return OS.WINDOWS;
        } else if (osName.contains("nix") || osName.contains("nux")) {
            return OS.LINUX;
        } else if (osName.contains("mac")) {
            return OS.MACOS;
        } else {
            return OS.UNKNOWN;
        }
    }

    public static String getOsKey() {
        return switch (getOS()) {
            case WINDOWS -> "windows";
            case LINUX -> "linux";
            case MACOS -> "osx";
            default -> "unknown";
        };
    }



}
