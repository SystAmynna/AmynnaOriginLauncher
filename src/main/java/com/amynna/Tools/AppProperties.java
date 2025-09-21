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
    public static final String APP_VERSION = "0.0.1";
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
    public static final File LAUNCHER_ROOT = new File(System.getProperty("user.home") + java.io.File.separator + "." + APP_NAME + java.io.File.separator);
    /**
     * Répertoire temporaire pour les fichiers téléchargés et autres opérations temporaires.
     */
    public static final File TEMP_DIR = new File(LAUNCHER_ROOT + "temp" + java.io.File.separator);
    /**
     * Répertoire pour stocker les signatures des fichiers.
     */
    public static final File SIGN_DIR = new File(LAUNCHER_ROOT + "sign" + java.io.File.separator);

    // CRYPTOGRAPHIE

    /**
     * Clé publique par défaut pour valider les signatures des fichiers de clés publiques de confiance.
     */
    public static final String DEFAULT_PUBLIC_KEY = "";
    /**
     * Nom associé à la clé publique par défaut.
     */
    public static final String DEFAULT_PUBLIC_KEY_OWNER = "Stiles";
    /**
     * Emplacement du côté serveur des signatures des fichiers.
     */
    public static final String SIGNATURE_LOCATION_ON_SERVER = REPO_SERVER_URL + File.separator + "signatures" + File.separator;
    /**
     * Répertoire pour stocker les signatures des fichiers.
     */
    public static final File SIGNATURE_DIR = new File(LAUNCHER_ROOT + "signatures" + java.io.File.separator);
    /**
     * Extension des fichiers de signature.
     */
    public static final String SIGNATURE_FILE_EXTENSION = ".sig";



}
