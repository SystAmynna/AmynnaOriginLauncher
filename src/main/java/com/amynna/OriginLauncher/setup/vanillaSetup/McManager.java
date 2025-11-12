package com.amynna.OriginLauncher.setup.vanillaSetup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Classe responsable de la gestion de l'installation de Minecraft Vanilla.
 */
public class McManager {

    /**
     * Manifeste des versions de Minecraft depuis Mojang.
     */
    private JSONObject mojangManifest;

    /**
     * Manifeste de la version spécifique de Minecraft.
     */
    private JSONObject minecraftManifest;

    private McManifestHandler mcManifestHandler;

    public McManager() {
        // Téléchargement du manifeste des versions de Minecraft depuis Mojang
        downloadMojangManifest();

        // Téléchargement du manifeste de la version spécifique de Minecraft
        downloadVersionManifest();

        // Initialisation du gestionnaire du manifeste Minecraft
        mcManifestHandler = new McManifestHandler(minecraftManifest);
    }

    /**
     * Installe Minecraft Vanilla.
     */
    public void setupMinecraft() {

        Logger.log(Logger.PURPLE + "Installation de Minecraft Vanilla...");

        mcManifestHandler.setupMinecraftFiles();

    }

    /**
     * Télécharge le manifeste des versions de Minecraft depuis Mojang.
     */
    private void downloadMojangManifest() {
        // Téléchargement du fichier manifest.json de Mojang
        File manifestFile = FileManager.downloadFile(AppProperties.MOJANG_MANIFEST_URL, AppProperties.MOJANG_MANIFEST.getPath());

        // Vérification du téléchargement
        if (manifestFile == null) {
            Logger.fatal("Manifest des versions de Minecraft introuvable. Veuillez vérifier votre connexion internet.");
        }
        assert manifestFile != null;

        // Lecture du fichier manifest.json
        try {
            mojangManifest = new JSONObject(new JSONTokener(new FileInputStream(manifestFile)));
        } catch (FileNotFoundException e) {
            Logger.fatal("Erreur lors de la lecture du manifest mojang : " + e.getMessage());
        }
    }

    /**
     * Télécharge le manifeste de la version spécifique de Minecraft.
     */
    private void downloadVersionManifest() {
        assert mojangManifest != null;

        // Varriables locales
        String versionUrl = null; // URL de la version spécifique de Minecraft
        String versionSha1 = null; // SHA1 de la version spécifique de Minecraft
        JSONArray versionsArray = mojangManifest.getJSONArray("versions"); // Tableau des versions disponibles

        // Recherche de la version spécifique
        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            if (versionObj.getString("id").equals(AppProperties.MINECRAFT_VERSION)) {
                versionUrl = versionObj.getString("url");
                versionSha1 = versionObj.getString("sha1");
                break;
            }
        }

        // Vérification de la disponibilité de la version
        if (versionUrl == null || versionSha1 == null) {
            Logger.fatal("Version de Minecraft " + AppProperties.MINECRAFT_VERSION + " indisponnible dans le manifeste.");
        }

        // Téléchargement et vérification du manifeste de la version spécifique
        File versionFile = FileManager.downloadFileAndVerifySha1(versionUrl, AppProperties.VERSION_MANIFEST.getPath(), versionSha1);

        // Vérification du téléchargement
        if (versionFile == null) {
            Logger.fatal("Manifest de la version de Minecraft introuvable. Veuillez vérifier votre connexion internet.");
        }
        assert versionFile != null;

        // Lecture du fichier de la version spécifique
        try {
            minecraftManifest = new JSONObject(new JSONTokener(new FileInputStream(versionFile)));
        } catch (FileNotFoundException e) {
            Logger.fatal("Erreur lors de la lecture du manifest de la version de Minecraft : " + e.getMessage());
        }
    }

    /** Répare l'installation de Minecraft Vanilla. */
    public void repairMinecraft() {
        mcManifestHandler.checkMinecraftFiles();
    }









}
