package com.amynna.Tools;

import java.io.File;
import java.nio.file.Paths;

/**
 * La classe {@code AppProperties} contient les propriétés et constantes globales de l'application.
 */
public final class AppProperties {

    // GÉNÉRAL

    /** Nom de l'application. */
    public static final String APP_NAME = "OriginRP";
    /** Version de l'application. */
    public static final String APP_VERSION = "0.2.3";
    /** Auteur de l'application. */
    public static final String APP_AUTHOR = "SystAmynna";

    // SERVEUR

    /** URL du serveur distant pour les mises à jour et la récupération des clés publiques. */
    public static final String REPO_SERVER_URL = "http://localhost:8000/v1";
    /** Emplacement du côté serveur des signatures des fichiers. */
    public static final String SIGNATURE_LOCATION_ON_SERVER = REPO_SERVER_URL + File.separator + "signatures" + File.separator;
    /** Valeur par défaut pour le mode multijoueur en jeu rapide. */
    public static final String QUICK_PLAY_MULTIPLAYER_VALUE = "...";

    // RÉPERTOIRES

    /** Répertoire racine du lanceur. */
    public static final File LAUNCHER_ROOT = new File(System.getProperty("user.home") + File.separator + "." + APP_NAME + File.separator);
    /** Répertoire temporaire pour les fichiers téléchargés et autres opérations temporaires. */
    public static final File TEMP_DIR = new File(LAUNCHER_ROOT + File.separator + "temp" + File.separator);
    /** Répertoire pour stocker les signatures des fichiers. */
    public static final File SIGNATURE_DIR = new File(LAUNCHER_ROOT + File.separator + "signatures" + File.separator);

    // CRYPTOGRAPHIE

    /** Clé publique par défaut pour valider les signatures des fichiers de clés publiques de confiance. */
    public static final String DEFAULT_PUBLIC_KEY = "MCowBQYDK2VwAyEAio7wTxwk3C2gm8KCfOPik7+mgspELKNmliJjTa2znPU=";
    /** Nom associé à la clé publique par défaut. */
    public static final String DEFAULT_PUBLIC_KEY_OWNER = "Stiles";
    /** Extension des fichiers de signature. */
    public static final String SIGNATURE_FILE_EXTENSION = ".sig";
    /** Type de KeyStore utilisé pour stocker les tokens d'authentification. */
    public static final String KEY_STORE_EXTENSION = ".p12";
    /** Emplacement local des clés publiques de confiance. */
    public static final File LOCAL_PRIVATE_KEYS_LOCATION = new File(LAUNCHER_ROOT + File.separator + "KEYS" + KEY_STORE_EXTENSION);

    // JAVA

    /** Version de Java requise pour exécuter le jeu. */
    public static final String JAVA_VERSION = "17";

    // AUTHENTIFICATION MICROSOFT

    /** Alias pour le token Microsoft dans le KeyStore. */
    public static final String MS_TOKEN_ALIAS = APP_NAME + "_MS_Token";
    /** Emplacement du fichier de token Microsoft. */
    public static final File MS_AUTH_TOKEN = new File(LAUNCHER_ROOT + File.separator + "MsAuthToken" + KEY_STORE_EXTENSION);

    // MINECRAFT

    /** Version de Minecraft à utiliser. */
    public static final String MINECRAFT_VERSION = "1.20.1";
    /** Répertoire d'installation de Minecraft. */
    public static final File MINECRAFT_DIR = new File(LAUNCHER_ROOT + File.separator + ".minecraft" + File.separator);
    /** Répertoire de la version spécifique de Minecraft. */
    public static final File MINECRAFT_VERSION_DIR = new File(MINECRAFT_DIR + File.separator + "versions" + File.separator + MINECRAFT_VERSION + File.separator);
    /** Fichier JAR du client Minecraft. */
    public static final File MINECRAFT_CLIENT = new File(MINECRAFT_VERSION_DIR + File.separator + MINECRAFT_VERSION + ".jar");
    /** Répertoire des bibliothèques Minecraft. */
    public static final File MINECRAFT_LIB_DIR = new File(MINECRAFT_DIR + File.separator + "libraries" + File.separator);
    /** Répertoire des assets Minecraft. */
    public static final File MINECRAFT_ASSETS_DIR = new File(MINECRAFT_DIR + File.separator + "assets" + File.separator);
    /** Répertoire des objets des assets Minecraft. */
    public static final File MINECRAFT_ASSETS_OBJECTS_DIR = new File(MINECRAFT_ASSETS_DIR + File.separator + "objects" + File.separator);
    /** Répertoire des index des assets Minecraft. */
    public static final File MINECRAFT_ASSETS_INDEX_DIR = new File(MINECRAFT_ASSETS_DIR + File.separator + "indexes" + File.separator);
    /** Répertoire des bibliothèques natives de Minecraft. */
    public static final File MINECRAFT_NATIVES_DIR = new File(MINECRAFT_VERSION_DIR + File.separator + "natives" + File.separator);

    // MOJANG

    /** URL du manifeste des versions de Minecraft. */
    public static final String MOJANG_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    /** Emplacement local du manifeste des versions de Minecraft. */
    public static final File MOJANG_MANIFEST = new File(TEMP_DIR + File.separator + "mojang_manifest.json");
    /** Emplacement local du manifeste de la version actuellement installée. */
    public static final File VERSION_MANIFEST = new File(MINECRAFT_VERSION_DIR + File.separator + "version.json");

    // FORGE

    /** Version de Forge à utiliser. */
    public static final String FORGE_VERSION = "47.4.0";
    /** Identifiant complet de la version Forge. */
    public static final String FORGE_ID = MINECRAFT_VERSION +"-forge-" + FORGE_VERSION;
    /** URL de l'installateur Forge. */
    public static final String FORGE_INSTALLER_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + MINECRAFT_VERSION + "-" + FORGE_VERSION + "/forge-" + MINECRAFT_VERSION + "-" + FORGE_VERSION + "-installer.jar";
    /** Emplacement du répertoire de version de Forge */
    public static final File FORGE_VERSION_DIR = new File(MINECRAFT_DIR + File.separator + "versions" + File.separator + FORGE_ID + File.separator);
    /** Emplacement du manifeste de Forge. */
    public static final File FORGE_MANIFEST = new File(FORGE_VERSION_DIR + File.separator + FORGE_ID + ".json");

    // OS

    /**
     * Obtient le type de système d'exploitation.
     *
     * @return {@code String} le type de système d'exploitation (windows, macos, linux, unknown).
     */
    public static String getOsType() {
            // Récupère le nom du système d'exploitation en minuscules
            String osName = System.getProperty("os.name").toLowerCase();

            // Détermine le type d'OS en fonction du nom
            if (osName.startsWith("windows")) {
                return "windows";
            } else if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
                return "osx";
            } else if (osName.startsWith("linux") || osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                return "linux";
            }

            // Si l'OS n'est pas reconnu, log une erreur fatale
            Logger.fatal("Votre OS n'a pas été reconnu : [" + osName + "]\n"+
                    "Linux, Windows et MacOS sont normalement supportés.\n" +
                    "Veuillez contacter un administrateur.");

            // Retourne "unknown" si l'OS n'est pas reconnu
            return "unknown";
        }
    /**
     * Obtient l'architecture du système d'exploitation.
     *
     * @return {@code String} l'architecture du système d'exploitation (x86, x64, arm, unknown).
     */
    public static String getOsArch() {
        // Récupère l'architecture du système en minuscules
        String arch = System.getProperty("os.arch").toLowerCase();

        // Détermine l'architecture en fonction de la valeur récupérée
        return switch (arch) {
            case "x86_64", "amd64" -> "x64";
            case "aarch64", "arm64" -> "arm";
            case "x86", "i386", "i486", "i586", "i686" -> "x86";
            default -> "unknown";
        };
    }
    /**
     * Recherche l'exécutable Java dans les répertoires définis dans la variable PATH.
     * @return {@code String} Le chemin vers l'exécutable Java actuel ou null si non trouvé
     */
    public static String foundJava() {
        String javaHome = System.getProperty("java.home");
        String javaExecutable = getOsType().equals("windows") ? "java.exe" : "java";
        return Paths.get(javaHome, "bin", javaExecutable).toString();
    }
    /**
     * Détermine le séparateur de chemin de classe (classpath) en fonction de l'OS.
     *
     * @return {@code String} Le séparateur (':' ou ';').
     */
    public static String getCpSeparator() {
        // En Java, le séparateur de classpath est stocké dans la propriété du système.
        return File.pathSeparator;
    }

}
