package com.amynna.OriginLauncher.setup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/** Gestionnaire du JDK pour le lanceur. */
public class JdkManager {

    /** Chemin vers le JDK utilisé par le lanceur. */
    private static String java;

    /** Type de compression de l'archive JDK (zip ou tar.gz). */
    private final String compressionType;
    /** Fichier de l'archive JDK téléchargée. */
    private final File jdkArchive;

    /** Manifeste JDK récupéré depuis l'API Adoptium. */
    private JSONObject manifest;

    /** Nom du JDK téléchargé. */
    private String jdkName;


    /** Obtient le chemin vers le JDK utilisé par le lanceur. */
    public static String getJava() {
        return java;
    }

    /** Constructeur du gestionnaire JDK. */
    protected JdkManager() {

        // .zip ou .tar.gz
        if (AppProperties.getOsType().equals("windows")) compressionType = "zip";
        else compressionType = "tar.gz";

        jdkArchive = new File(AppProperties.TEMP_DIR, "jdk." + compressionType);


    }

    /** Configure le JDK pour le lanceur. */
    protected void jdkSetup() {
        // Définir la valeur par défaut
        java = AppProperties.foundJava();

        // Vérifier si un JDK propre est nécessaire
        if (!ownJdkNeeded()) {
            Logger.log(Logger.GREEN + "Version de Java actuelle valide: " + java);
            return;
        }

        Logger.log("Version de Java actuelle non valide pour Minecraft...");

        // Récupérer le manifeste JDK
        downloadManifest();
        // Vérifier si le JDK propre est déjà téléchargé et valide
        if (checkJdk()) {
            Logger.log(Logger.GREEN + "JDK propre déjà téléchargé et valide.");
            java = getOwnJdkPath();
            return;
        }

        Logger.log("Téléchargement du JDK propre...");

        downloadJdk();
        installJdk();

        Logger.log(Logger.GREEN + "JDK propre installé avec succès.");

    }

    /** Détermine si un JDK propre est nécessaire en fonction de la version actuelle de Java. */
    private boolean ownJdkNeeded() {

        final String currentVersion = System.getProperty("java.version");
        if (currentVersion == null) return true;

        final String[] versionParts = currentVersion.split(Pattern.quote("."));
        if (versionParts.length < 2) return true;

        final String majorVersion = versionParts[0];

        Logger.log("Version actuelle de Java: " + currentVersion + " (majeur: " + majorVersion + ")");

        return !majorVersion.equals(AppProperties.JAVA_VERSION);

    }

    /** Vérifie si le JDK propre est déjà téléchargé et valide. */
    private boolean checkJdk() {
        String hypoteticalJdkPath = getOwnJdkPath();
        File jdkBin = new File(hypoteticalJdkPath);
        if (!jdkBin.exists() || !jdkBin.isFile()) return false;

        // essaye de lancer le JDK pour vérifier son fonctionnement
        try {
            Process process = new ProcessBuilder(hypoteticalJdkPath, "-version")
                    .redirectErrorStream(true)
                    .start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // JDK fonctionne correctement
                return true;
            } else {
                Logger.log("Le JDK propre ne fonctionne pas correctement (code de sortie: " + exitCode + ").");
            }
        } catch (IOException | InterruptedException e) {
            Logger.log("Erreur lors de la vérification du JDK propre : " + e.getMessage());
            Thread.currentThread().interrupt();
        }

        return false;
    }

    /** Télécharge le manifeste JDK depuis l'API Adoptium. */
    private void downloadManifest() {

        // données système
        final String osName =  AppProperties.getOsType();
        final String osArch = AppProperties.getOsArch();

        // construire le lien de téléchargement
        final String link = buildLink(osName, osArch, compressionType);
        // récupérer le manifeste JDK
        manifest = getJdkManifest(link);
        assert manifest != null;

        jdkName = manifest.getString("release_name");

    }

    /** Télécharge le JDK depuis l'API Adoptium. */
    private void downloadJdk() {


        final JSONObject binary = manifest.getJSONObject("binary");
        assert binary != null;
        final JSONObject packageInfo = binary.getJSONObject("package");
        assert packageInfo != null;
        // données de téléchargement
        final String downloadLink = packageInfo.getString("link");
        final String expectedChecksum = packageInfo.getString("checksum");
        final long fileSize = packageInfo.getLong("size");

        // télécharger le JDK
        FileManager.downloadFileAndVerifySha(downloadLink, jdkArchive.getPath(), expectedChecksum, FileManager.SHA256);
        if (!jdkArchive.exists()) Logger.fatal("Le téléchargement du JDK a échoué.");
        else if (!(jdkArchive.length() == fileSize)) Logger.fatal("Le téléchargement du JDK est invalide (taille incorrecte).");

    }


    /** Installe le JDK en décompressant l'archive téléchargée. */
    private void installJdk() {
        if (compressionType.equals("zip")) {
            FileManager.unzip(jdkArchive, AppProperties.LAUNCHER_ROOT);
        } else {
            FileManager.untarGz(jdkArchive, AppProperties.LAUNCHER_ROOT);
        }

        // redéfinir le chemin vers le JDK
        java = getOwnJdkPath();
    }

    /** Obtient le chemin vers le JDK propre installé. */
    private String getOwnJdkPath() {
        return AppProperties.LAUNCHER_ROOT + File.separator + jdkName + File.separator + "bin" + File.separator + "java" +
                (AppProperties.getOsType().equals("windows") ? ".exe" : "");
    }

    /** Construit le lien de téléchargement du JDK depuis l'API Adoptium. */
    private String buildLink(String osName, String osArch, String compressionType) {
        // Mapper l'OS vers le format Adoptium
        String adoptiumOs;
        switch (osName) {
            case "windows":
                adoptiumOs = "windows";
                break;
            case "osx":
                adoptiumOs = "mac";
                break;
            case "linux":
                adoptiumOs = "linux";
                break;
            default:
                Logger.log("Unsupported OS: " + osName);
                return null;
        }

        // Mapper l'architecture vers le format Adoptium
        String adoptiumArch;
        switch (osArch) {
            case "x64":
                adoptiumArch = "x64";
                break;
            case "arm":
                adoptiumArch = "aarch64";
                break;
            case "x86":
                adoptiumArch = "x32";
                break;
            default:
                Logger.log("Unsupported architecture: " + osArch);
                return null;
        }


        return String.format(
                "https://api.adoptium.net/v3/assets/latest/%s/hotspot?architecture=%s&image_type=jdk&os=%s&vendor=eclipse",
                AppProperties.JAVA_VERSION,
                adoptiumArch,
                adoptiumOs
        );
    }

    /** Récupère le manifeste JDK depuis l'API Adoptium. */
    private JSONObject getJdkManifest(String link) {

        HttpClient client = HttpClient.newBuilder().
                version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(link))
                .header("Accept", "application/json")
                .GET()
                .build();

        String jsonResponse = null;

        try {

            HttpResponse<String> reponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (reponse.statusCode() == HttpURLConnection.HTTP_OK) {
                jsonResponse = reponse.body();
            } else {
                Logger.log("Failed to fetch JDK manifest. HTTP Status: " + reponse.statusCode());
                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            Logger.fatal("La connexion a échoué lors de la récupération du manifeste JDK : " + e.getMessage());
        }

        assert jsonResponse != null;

        JSONArray assets = new JSONArray(jsonResponse);

        if (assets.isEmpty()) Logger.fatal("JDK manifeste n'existe pas");

        return assets.getJSONObject(0);


    }

}
