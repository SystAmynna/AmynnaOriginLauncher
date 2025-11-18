package com.amynna.OriginLauncher.setup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONObject;

import java.io.File;

public class ClientManager {


    private final JSONObject clientData;

    private final McClient mcClient;

    /** Classe représentant le client Minecraft. */
    private record McClient(String url, String sha1, int size, File file) {
        private McClient {}
    }

    protected ClientManager(JSONObject downloads) {

        assert downloads != null;
        clientData = downloads.getJSONObject("client");
        assert clientData != null;

        mcClient = parseMcClient();
    }

    /** Récupère les informations du client Minecraft depuis le manifeste.
     *
     * @return Le client Minecraft.
     */
    private McClient parseMcClient() {

        // Extraction des données
        String url = clientData.getString("url");
        String sha1 = clientData.getString("sha1");
        int size = clientData.getInt("size");
        File file = AppProperties.MINECRAFT_CLIENT;

        // Création et retour de l'objet McClient
        return new McClient(url, sha1, size, file);
    }

    /**
     * Télécharge le client Minecraft s'il n'est pas déjà présent.
     */
    protected void downloadMcClient() {
        // Vérification légère avant le téléchargement
        if (lightCheckMcClient()) return;

        // Téléchargement du client Minecraft
        FileManager.downloadFileAndVerifySha(
                mcClient.url,
                mcClient.file.getPath(),
                mcClient.sha1,
                FileManager.SHA1
        );
    }

    /**
     * Vérifie l'intégrité du client Minecraft.
     * Si le fichier est manquant ou corrompu, il est retéléchargé.
     */
    protected void checkMcClient() {

        Logger.logc("Vérification du Client... ");

        // Vérification légère
        lightCheckMcClient();

        // Vérification du SHA1
        String fileSha1 = FileManager.calculSHA(mcClient.file, FileManager.SHA1);
        if (fileSha1 != null && fileSha1.equals(mcClient.sha1)) {
            Logger.log(Logger.GREEN + "[OK]");
            return;
        }

        Logger.log(Logger.RED + "[CORROMPU]");

        // Suppression du fichier corrompu
        FileManager.deleteFileIfExists(mcClient.file);
        // Retéléchargement du client Minecraft
        downloadMcClient();
    }

    /**
     * Effectue une vérification légère du client Minecraft.
     *
     * @return true si le fichier existe et sa taille correspond, false sinon.
     */
    protected boolean lightCheckMcClient() {
        return mcClient != null
                && mcClient.file != null
                && mcClient.file.exists()
                && mcClient.file.length() == mcClient.size;
    }




}
