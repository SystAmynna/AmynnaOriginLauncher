package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.tools.Config;
import fr.amynna.OriginLauncher.tools.Web;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;

public class App {

    public static final String NAME = "Origin";
    public static final String VERSION = "1.20.1";

    public boolean existOnSystem() {
        // Verifier si le répertoire de l'application Origin existe
        return Config.getWorkingDir().resolve(NAME).toFile().exists();
    }

    public void setup() {
        Config.getWorkingDir().resolve(NAME).toFile().mkdirs(); // Créer le répertoire de l'application Origin
        Config.getLauncherDir().toFile().mkdirs(); // Créer le répertoire du launcher
        Config.getMinecraftDir().toFile().mkdirs(); // Créer le répertoire de Minecraft

        // télécharger le manifest des versions de mojang
        String versionManifestUrl = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
        Path manifestPath = Paths.get(Config.getMinecraftDir(), "version_manifest.json");

        if (!Web.downloadFile(versionManifestUrl, manifestPath)) {
            throw new RuntimeException("Failed to download version manifest.");
        } else {
            System.out.println("Version manifest downloaded successfully.");
        }

        // charger le manifest
        JSONObject mojangManifest = new JSONObject(manifestPath.toFile().toPath().toUri().toURL().openStream());
        JSONObject versionManifest;
        for (JSONObject i : mojangManifest.keySet()) {
            if (VERSION.equals(i.get("id"))) {
                versionManifest = i;
                break;
            }
        }
        if (versionManifest == null) {
            throw new RuntimeException("Version " + VERSION + " not found in manifest.");
        }

        // télécharger les fichiers nécessaires à la version
        Path versionFilePath = Paths.get(Config.getTempDir().toString(), VERSION + ".json");
        String versionUrl = versionManifest.getString("url");
        if (!Web.downloadFile(versionUrl, versionFilePath)) {
            throw new RuntimeException("Failed to download version file: " + VERSION);
        } else {
            System.out.println("Version file downloaded successfully: " + VERSION);
        }




    }



}
