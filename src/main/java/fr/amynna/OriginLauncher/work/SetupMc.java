package fr.amynna.OriginLauncher.work;

import fr.amynna.OriginLauncher.data.Config;
import fr.amynna.OriginLauncher.data.Proprieties;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;
import fr.amynna.OriginLauncher.tools.Web;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SetupMc {

    private static final String MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private static JSONObject versionManifest = null;
    private static JSONObject assetIndex = null;

    public static void process() {

        if (checkInstallation()) {
            Printer.printInfo("Minecraft est déjà installé.");
            return;
        }

        Printer.printInfo("Installation de Minecraft " + Proprieties.MINECRAFT_VERSION + "...");

        install();

        Config.updateBooleanConfig("mcInstalled", true);

    }

    private static boolean checkInstallation() {
        return Config.getBooleanConfig("mcInstalled");
    }


    private static void install() {

        JSONObject mojangManifest = downloadMojangManifest();
        versionManifest = downloadVersionManifest(mojangManifest);

        downloadClientJar(versionManifest);

        downloadLibraries(versionManifest);

        downloadAssets(versionManifest);


        Printer.printInfo("Installation de Minecraft " + Proprieties.MINECRAFT_VERSION + " terminée.");
    }

    private static JSONObject downloadMojangManifest() {

        Printer.printInfo("Téléchargement du manifest des versions de Minecraft...");

        File manifestFile = Web.downloadFile(MOJANG_MANIFEST, Proprieties.ROOT_PATH + File.separator + "mojang_manifest.json");

        if (manifestFile == null) {
            Printer.fatalError("Manifest des versions de Minecraft introuvable. Veuillez vérifier votre connexion internet.");
        }
        assert manifestFile != null;
        try {
            return new JSONObject(new JSONTokener(new FileInputStream(manifestFile)));
        } catch (FileNotFoundException e) {
            Printer.fatalError("Erreur lors de la lecture du manifest mojang : " + e.getMessage());
            return null;
        }
    }

    private static JSONObject downloadVersionManifest(JSONObject manifest) {

        Printer.printInfo("Téléchargement du manifest de la version de Minecraft...");

        String versionUrl = null;
        var versionsArray = manifest.getJSONArray("versions");

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            if (versionObj.getString("id").equals(Proprieties.MINECRAFT_VERSION)) {
                Printer.printInfo("Version trouvée : " + versionObj.getString("id"));
                versionUrl = versionObj.getString("url");
                break;
            }
        }

        if (versionUrl == null) {
            Printer.fatalError("Version de Minecraft " + Proprieties.MINECRAFT_VERSION + " introuvable dans le manifeste.");
        }

        File versionFile = Web.downloadFile(versionUrl, Proprieties.ROOT_PATH + File.separator + "version.json");

        try {
            return new JSONObject(new JSONTokener(new FileInputStream(versionFile)));
        } catch (FileNotFoundException e) {
            Printer.fatalError("Erreur lors de la lecture du manifest de la version de Minecraft : " + e.getMessage());
            return null;
        }
    }

    private static void downloadClientJar(JSONObject versionManifest) {

        Printer.printInfo("Téléchargement du client.jar...");

        JSONObject downloads = versionManifest.getJSONObject("downloads");
        JSONObject client = downloads.getJSONObject("client");

        String clientUrl = client.getString("url");
        String clientSha1 = client.getString("sha1");

        Path clientJarPath = Paths.get(Proprieties.MC_PATH, "versions", Proprieties.MINECRAFT_VERSION, Proprieties.MINECRAFT_VERSION + ".jar");

        boolean success = false;
        int maxRetries = 3;

        for (int i = 0; i < maxRetries && !success; i++) {
            try {
                FileManager.downloadAndVerifyFile(clientUrl, clientJarPath.toString(), clientSha1);
                success = Files.exists(clientJarPath) && Files.isRegularFile(clientJarPath) && Files.size(clientJarPath) > 0;
                if (success) break;
            } catch (Exception e) {
                Printer.printWarning("Échec du téléchargement client.jar, tentative " + (i + 1) + "/" + maxRetries + "...");
            }
        }
        if (!success) {
            Printer.fatalError("Impossible de créer le répertoire pour le fichier client.jar. Veuillez vérifier les permissions.");
        }


    }

    private static void downloadLibraries(JSONObject versionManifest) {
        // Implémentation pour télécharger les bibliothèques nécessaires
        Printer.printInfo("Téléchargement des bibliothèques...");

        JSONArray libraries = versionManifest.getJSONArray("libraries");

        for (int i = 0; i < libraries.length(); i++) {
            JSONObject lib = libraries.getJSONObject(i);
            if (!lib.has("downloads")) continue;

            JSONObject artifact = lib.getJSONObject("downloads").optJSONObject("artifact");
            if (artifact == null) continue;

            String url = artifact.getString("url");
            String path = artifact.getString("path");
            String sha1 = artifact.getString("sha1");

            Path libFile = Paths.get(Proprieties.MC_PATH, "libraries", path);
            FileManager.downloadAndVerifyFile(url, libFile.toString(), sha1);
        }

    }

    private static void downloadAssets(JSONObject versionManifest) {
        Printer.printInfo("Téléchargement des assets...");

        assetIndex = versionManifest.getJSONObject("assetIndex");
        String assetUrl = assetIndex.getString("url");

        JSONObject assetsJson = null;
        try {
            assetsJson = new JSONObject(
                    new String(new URL(assetUrl).openStream().readAllBytes(), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            Printer.fatalError("Erreur lors du téléchargement du fichier d'assets : " + e.getMessage());
            return;
        }

        JSONObject objects = assetsJson.getJSONObject("objects");
        for (String key : objects.keySet()) {
            JSONObject asset = objects.getJSONObject(key);
            String hash = asset.getString("hash");
            String subDir = hash.substring(0, 2);
            String url = "https://resources.download.minecraft.net/" + subDir + "/" + hash;

            Path assetFile = Paths.get(Proprieties.MC_PATH, "assets", "objects", subDir, hash);
            FileManager.downloadAndVerifyFile(url, assetFile.toString(), hash); // vérifie SHA1
        }

    }

}
