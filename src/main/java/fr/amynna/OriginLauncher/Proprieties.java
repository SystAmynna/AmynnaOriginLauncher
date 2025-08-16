package fr.amynna.OriginLauncher;

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


}
