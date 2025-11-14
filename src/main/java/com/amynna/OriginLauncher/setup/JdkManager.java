package com.amynna.OriginLauncher.setup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Logger;

import java.io.File;
import java.util.Objects;
import java.util.regex.Pattern;

/** Gestionnaire du JDK pour le lanceur. */
public class JdkManager {

    /** Chemin vers le JDK utilisé par le lanceur. */
    private static String java;

    /** Obtient le chemin vers le JDK utilisé par le lanceur. */
    public static String getJava() {
        return java;
    }

    /** Configure le JDK pour le lanceur. */
    protected void jdkSetup() {
        // Définir la valeur par défaut
        java = AppProperties.foundJava();

        // Vérifier si un JDK propre est nécessaire
        if (!ownJdkNeeded()) {
            Logger.log("Using system JDK: " + java);
            return;
        }






    }

    /** Détermine si un JDK propre est nécessaire en fonction de la version actuelle de Java. */
    private boolean ownJdkNeeded() {

        final String currentVersion = System.getProperty("java.version");
        if (currentVersion == null) return true;

        final String[] versionParts = currentVersion.split(Pattern.quote("."));
        if (versionParts.length < 2) return true;

        final String majorVersion = versionParts[0];

        Logger.log("Current Java version: " + currentVersion + " (major: " + majorVersion + ")");

        return !majorVersion.equals(AppProperties.JAVA_VERSION);

    }

    private void downloadJdk() {

        // .zip ou .tar.gz
        String compressionType;
        if (AppProperties.getOsType().equals("windows")) compressionType = "zip";
        else compressionType = "tar.gz";

        // destination du téléchargement
        File downloadTarget = new File(AppProperties.TEMP_DIR, "jdk" + compressionType);

        final String osName =  AppProperties.getOsType();
        final String osArch = AppProperties.getOsArch();



    }

    private String buildLink() {


    }

}
