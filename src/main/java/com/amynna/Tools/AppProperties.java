package com.amynna.Tools;

import java.io.File;

/**
 * Classe utilitaire pour la gestion des propriétés de l'application.
 */
public final class AppProperties {

    // GÉNÉRAL

    /**
     * Nom de l'application.
     */
    public static final String APP_NAME = "OriginRP";
    /**
     * Version de l'application.
     */
    public static final String APP_VERSION = "0.1.1";
    /**
     * Auteur de l'application.
     */
    public static final String APP_AUTHOR = "SystAmynna";
    /**
     * URL du serveur distant pour les mises à jour et la récupération des clés publiques.
     */
    public static final String REPO_SERVER_URL = "http://localhost:8000";

    // RÉPERTOIRES

    /**
     * Répertoire racine du lanceur.
     */
    public static final File LAUNCHER_ROOT = new File(System.getProperty("user.home") + File.separator + "." + APP_NAME + File.separator);
    /**
     * Répertoire temporaire pour les fichiers téléchargés et autres opérations temporaires.
     */
    public static final File TEMP_DIR = new File(LAUNCHER_ROOT + File.separator + "temp" + File.separator);
    /**
     * Répertoire pour stocker les signatures des fichiers.
     */
    public static final File SIGNATURE_DIR = new File(LAUNCHER_ROOT + File.separator + "signatures" + File.separator);

    // CRYPTOGRAPHIE

    /**
     * Clé publique par défaut pour valider les signatures des fichiers de clés publiques de confiance.
     */
    public static final String DEFAULT_PUBLIC_KEY = "-----------------------";
    /**
     * Nom associé à la clé publique par défaut.
     */
    public static final String DEFAULT_PUBLIC_KEY_OWNER = "Stiles";
    /**
     * Emplacement du côté serveur des signatures des fichiers.
     */
    public static final String SIGNATURE_LOCATION_ON_SERVER = REPO_SERVER_URL + File.separator + "signatures" + File.separator;
    /**
     * Extension des fichiers de signature.
     */
    public static final String SIGNATURE_FILE_EXTENSION = ".sig";
    /**
     * Type de KeyStore utilisé pour stocker les tokens d'authentification.
     */
    public static final String KEY_STORE_EXTENSION = ".p12";
    /**
     * Emplacement local des clés publiques de confiance.
     */
    public static final File LOCAL_PRIVATE_KEYS_LOCATION = new File(LAUNCHER_ROOT + File.separator + "KEYS" + KEY_STORE_EXTENSION);

    // AUTHENTIFICATION MICROSOFT

    /**
     * Alias pour le token Microsoft dans le KeyStore.
     */
    public static final String MS_TOKEN_ALIAS = APP_NAME + "_MS_Token";
    /**
     * Emplacement du fichier de token Microsoft.
     */
    public static final File MS_AUTH_TOKEN = new File(LAUNCHER_ROOT + File.separator + "MsAuthToken" + KEY_STORE_EXTENSION);
    /**
     * L'URL de l'API de profil Minecraft pour récupérer le XUID.
     */
    public static final String PROFILE_API_URL = "https://api.minecraftservices.com/minecraft/profile";

    // MOJANG

    /**
     * URL du manifeste des versions de Minecraft.
     */
    public static final String MOJANG_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    /**
     * Emplacement local du manifeste des versions de Minecraft.
     */
    public static final File MOJANG_MANIFEST = new File(TEMP_DIR + File.separator + "mojang_manifest.json");
    /**
     * Emplacement local du manifeste de la version actuellement installée.
     */
    public static final File VERSION_MANIFEST = new File(TEMP_DIR + File.separator + "version.json");

    // MINECRAFT

    /**
     * Répertoire d'installation de Minecraft.
     */
    public static final File MINECRAFT_DIR = new File(LAUNCHER_ROOT + File.separator + ".minecraft" + File.separator);
    /**
     * Version de Minecraft à utiliser.
     */
    public static final String MINECRAFT_VERSION = "1.20.1";


    // OS

    /**
     * Obtient le type de système d'exploitation.
     *
     * @return le type de système d'exploitation (windows, macos, linux, unknown).
     */
    public static String getOsType() {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.startsWith("windows")) {
                return "windows";
            } else if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
                return "macos";
            } else if (osName.startsWith("linux")) {
                return "linux";
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                // Catégorie de repli pour d'autres systèmes de type Unix
                return "linux";
            }

            return "unknown";
        }
    /**
     * Obtient l'architecture du système d'exploitation.
     *
     * @return l'architecture du système d'exploitation (x86, x64, arm, unknown).
     */
    public static String getOsArch() {
        String arch = System.getProperty("os.arch").toLowerCase();

        if (arch.contains("86")) return "x86";
        else if (arch.contains("64")) return "x64";
        else if (arch.contains("arm")) return "arm";
        else return "unknown";
    }



}
