package fr.amynna.OriginLauncher.work;

import fr.amynna.OriginLauncher.data.Config;
import fr.amynna.OriginLauncher.data.Proprieties;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;
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

/**
 * Classe {@code SetupMc} gère l'installation de Minecraft en téléchargeant les fichiers nécessaires
 * à partir des manifestes de Mojang et en les organisant dans la structure de répertoires appropriée.
 */
public class SetupMc {

    /**
     * URL du manifeste des versions de Minecraft de Mojang.
     * Utilisé pour récupérer les informations sur les versions disponibles.
     */
    private static final String MOJANG_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    /**
     * Chemin du fichier manifeste de Mojang local.
     * Utilisé pour stocker le manifeste téléchargé.
     */
    public static final String MOJANG_MANIFEST_PATH = Proprieties.ROOT_PATH + File.separator + "mojang_manifest.json";
    /**
     * Chemin du fichier manifeste de la version spécifique de Minecraft.
     * Utilisé pour stocker le manifeste de la version téléchargé.
     */
    public static final String VERSION_MANIFEST_PATH = Proprieties.ROOT_PATH + File.separator + "version.json";


    /**
     * Méthode principale pour lancer le processus d'installation de Minecraft.
     * Vérifie d'abord si Minecraft est déjà installé, puis procède à l'installation si ce n'est pas le cas.
     */
    public static void process() {

        if (checkInstallation()) {
            Printer.info("Minecraft est déjà installé.");
            return;
        }

        Printer.info("Installation de Minecraft " + Proprieties.MINECRAFT_VERSION + "...");

        install();

        Config.updateBooleanConfig("mcInstalled", true);

    }

    /**
     * Vérifie si Minecraft est déjà installé en consultant la configuration.
     *
     * @return true si Minecraft est installé, false sinon
     */
    private static boolean checkInstallation() {
        return Config.getBooleanConfig("mcInstalled");
    }

    /**
     * Procède à l'installation de Minecraft en téléchargeant les manifestes, les fichiers nécessaires,
     * et en organisant la structure de répertoires.
     */
    private static void install() {

        Printer.info("Installation de Minecraft " + Proprieties.MINECRAFT_VERSION + " en cours...");

        JSONObject mojangManifest = downloadMojangManifest();
        JSONObject versionManifest = downloadVersionManifest(mojangManifest);

        downloadClientJar(versionManifest);
        downloadLibraries(versionManifest);
        downloadAssets(versionManifest);

        Printer.info("Installation de Minecraft " + Proprieties.MINECRAFT_VERSION + " terminée.");
    }

    /**
     * Télécharge le manifeste des versions de Minecraft depuis Mojang.
     *
     * @return un objet JSON représentant le manifeste des versions
     */
    private static JSONObject downloadMojangManifest() {

        Printer.info("Téléchargement du manifest des versions de Minecraft...");

        File manifestFile = FileManager.downloadFile(MOJANG_MANIFEST, MOJANG_MANIFEST_PATH);

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

    /**
     * Télécharge le manifeste de la version spécifique de Minecraft.
     *
     * @param manifest l'objet JSON du manifeste des versions
     * @return un objet JSON représentant le manifeste de la version spécifique
     */
    private static JSONObject downloadVersionManifest(JSONObject manifest) {

        Printer.info("Téléchargement du manifest de la version de Minecraft...");

        String versionUrl = null;
        var versionsArray = manifest.getJSONArray("versions");

        for (int i = 0; i < versionsArray.length(); i++) {
            JSONObject versionObj = versionsArray.getJSONObject(i);
            if (versionObj.getString("id").equals(Proprieties.MINECRAFT_VERSION)) {
                Printer.info("Version trouvée : " + versionObj.getString("id"));
                versionUrl = versionObj.getString("url");
                break;
            }
        }

        if (versionUrl == null) {
            Printer.fatalError("Version de Minecraft " + Proprieties.MINECRAFT_VERSION + " introuvable dans le manifeste.");
        }

        File versionFile = FileManager.downloadFile(versionUrl, VERSION_MANIFEST_PATH);

        try {
            return new JSONObject(new JSONTokener(new FileInputStream(versionFile)));
        } catch (FileNotFoundException e) {
            Printer.fatalError("Erreur lors de la lecture du manifest de la version de Minecraft : " + e.getMessage());
            return null;
        }
    }

    /**
     * Télécharge le fichier client.jar de Minecraft et le place dans le répertoire approprié.
     *
     * @param versionManifest l'objet JSON du manifeste de la version de Minecraft
     */
    private static void downloadClientJar(JSONObject versionManifest) {

        Printer.info("Téléchargement du client.jar...");

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
                Printer.warning("Échec du téléchargement client.jar, tentative " + (i + 1) + "/" + maxRetries + "...");
            }
        }
        if (!success) {
            Printer.fatalError("Impossible de créer le répertoire pour le fichier client.jar. Veuillez vérifier les permissions.");
        }


    }

    /**
     * Télécharge les bibliothèques nécessaires pour Minecraft et les place dans le répertoire approprié.
     *
     * @param versionManifest l'objet JSON du manifeste de la version de Minecraft
     */
    private static void downloadLibraries(JSONObject versionManifest) {
        // Implémentation pour télécharger les bibliothèques nécessaires
        Printer.info("Téléchargement des bibliothèques...");

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

    /**
     * Télécharge les assets de Minecraft et les place dans le répertoire approprié.
     *
     * @param versionManifest l'objet JSON du manifeste de la version de Minecraft
     */
    private static void downloadAssets(JSONObject versionManifest) {
        Printer.info("Téléchargement des assets...");

        JSONObject assetIndex = versionManifest.getJSONObject("assetIndex");
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
