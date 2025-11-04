package com.amynna.OriginLauncher.setup.vanillaSetup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Classe responsable de la gestion des assets Minecraft.
 */
public class McAssetManager {

    // ----[ ATTRIBUTS ]----

    public static final String ASSETS_URL_BASE = "https://resources.download.minecraft.net/";

    /** Valeur des assets (pour le lancement) */
    public final String assets;
    /** Index des assets */
    private final JSONObject assetIndex;
    /** Manifeste des assets */
    private final JSONObject assetIndexObjects;
    /** Nom de l'index des assets */
    public final String assetIndexName;

    /** Taille totale des assets */
    private final int totalAssetsSize;

    /** Liste des objets assets */
    private final List<AssetObject> assetObjects;
    /** Classe représentant un objet asset. */
    private record AssetObject(String name, String hash, int size, File file) {

        /**
         * Construit l'URL complète de téléchargement de l'asset.
         * Format: https://resources.download.minecraft.net/ <2_chars_hash> / <hash_complet>
         * @return L'URL complète de l'asset.
         */
        public String getDownloadUrl() {
            String hashPrefix = hash.substring(0, 2);
            return ASSETS_URL_BASE + hashPrefix + File.separator + hash;
        }

        /** Télécharge l'asset et vérifie son SHA-1 (hash). */
        public void download() {
            String downloadUrl = getDownloadUrl();
            FileManager.downloadFileAndVerifySha1(downloadUrl, file.getPath(), hash);
        }

        /** Vérifie l'intégrité de l'asset en comparant le hash (SHA-1). */
        public boolean check() {
            if (!lightCheck()) return false;
            // Le hash des assets est bien un SHA-1
            String fileSha1 = FileManager.calculSHA1(file);
            assert fileSha1 != null;
            return fileSha1.equals(hash);
        }

        /** Vérifie si le fichier existe et si sa taille correspond. */
        public boolean lightCheck() {
            return file.exists() && file.length() == size && hash.equals(file.getName());
        }
    }

    /** Constructeur de la classe McAssetManager.
     *
     * @param assetsVal Valeur des assets.
     * @param assetIndexJson Index des assets au format JSON.
     */
    public McAssetManager(String assetsVal, JSONObject assetIndexJson) {
        // résolution de l'index des assets
        this.assetIndex = assetIndexJson;
        assert assetIndex != null;

        // extraction de la valeur des assets
        String id = assetIndex.getString("id");
        String url = assetIndex.getString("url");
        int size = assetIndex.getInt("size");
        totalAssetsSize = assetIndex.getInt("totalSize");
        String sha1 = assetIndex.getString("sha1");

        // résolution de la valeur des assets
        assert assetsVal.equals(id);
        this.assets = assetsVal;

        // téléchargement du fichier index des assets
        String assetsManifestPath = AppProperties.MINECRAFT_ASSETS_DIR.getPath();
        File assetIndexFile = FileManager.downloadFileAndVerifySha1(url, assetsManifestPath, sha1);
        assert assetIndexFile != null;
        assert assetIndexFile.exists();
        assert assetIndexFile.length() == size;

        // nom de l'index des assets
        this.assetIndexName = assetIndexFile.getName();

        // lecture du fichier index des assets
        JSONObject jsonFile = FileManager.openJsonFile(assetIndexFile);
        assert jsonFile != null;
        this.assetIndexObjects = jsonFile.getJSONObject("objects");

        // initialisation de la liste des assets
        this.assetObjects = new LinkedList<>();

        // création de la liste des assets
        makeAssetList();
    }

    // ----[ MÉTHODES ]----

    /**
     * Crée une instance d'AssetObject à partir des données JSON.
     * Détermine le chemin de stockage de l'asset dans le dossier 'objects'.
     *
     * @param assetName La clé de l'asset (ex: "icons/icon_128x128.png").
     * @param assetData L'objet JSON contenant "hash" et "size".
     * @return Un AssetObject.
     */
    private AssetObject parseAssetObject(String assetName, JSONObject assetData) {

        // 1. Extraction des données
        String hash = assetData.getString("hash");
        int size = assetData.getInt("size");

        // 2. Détermination du chemin de stockage basé sur le hash
        String hashPrefix = hash.substring(0, 2);

        // Chemin relatif: <2_chars_hash> / <hash_complet>
        File hashDir = new File(AppProperties.MINECRAFT_ASSETS_OBJECTS_DIR, hashPrefix);

        // Chemin final du fichier
        File assetFile = new File(hashDir, hash);

        // 3. Assurez-vous que le dossier de préfixe existe
        FileManager.createDirectoriesIfNotExist(hashDir.getPath());

        // 4. Retourner l'instance
        return new AssetObject(assetName, hash, size, assetFile);
    }

    /**
     * Remplit la liste des AssetObject à partir de l'index des assets.
     */
    private void makeAssetList() {
        // Vide la liste avant de la remplir
        assetObjects.clear();

        // Parcourt tous les assets définis dans l'index
        for (String assetName : assetIndexObjects.keySet()) {
            // Récupère le JSON de l'asset
            JSONObject assetJson = assetIndexObjects.getJSONObject(assetName);
            // Construit l'objet AssetObject
            AssetObject asset = parseAssetObject(assetName, assetJson);
            // Ajoute l'asset à la liste
            assetObjects.add(asset);
        }
    }

    // ---[ MÉTHODES PUBLIQUES ]----

    /**
     * Télécharge tous les assets qui ne sont pas encore présents ou corrompus.
     */
    public void downloadAllAssets() {
        for (AssetObject asset : assetObjects) {
            if (!asset.lightCheck()) asset.download();
        }
    }

    /**
     * Vérifie l'intégrité de tous les assets.
     * Retélécharge ceux qui sont corrompus.
     */
    public void checkAllAssets() {
        int processSize = 0;
        for (AssetObject asset : assetObjects) {
            if (!asset.check()) {
                Logger.log("L'asset' " + asset.name + " est corrompue ou manquant. Téléchargement...");
                asset.download();
            }
            assert asset.size == asset.file.length();
            processSize += asset.size;
        }
        assert processSize == totalAssetsSize;
    }


}
