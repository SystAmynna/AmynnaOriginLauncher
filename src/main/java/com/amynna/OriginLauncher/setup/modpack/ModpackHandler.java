package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.OriginLauncher.AdminIdentificator;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import com.amynna.Tools.SignedFile;
import org.json.JSONObject;

/** La classe {@code ModpackHandler} gère les opérations liées aux modpacks dans le lanceur. */
public class ModpackHandler {

    /** Gestionnaire des fichiers du modpack */
    private MpFilesManager mpFilesManager;

    /** Manifeste du modpack */
    private final JSONObject modpackManifest;

    /** Manifeste admin du modpack */
    private final JSONObject modpackAdminManifest;


    public ModpackHandler() {

        // Téléchargement et ouverture du manifeste du modpack
        String onServerUrl = AppProperties.MODPACK_DIR_ON_SERVER + "modpack_manifest.json";
        SignedFile modpackFile = FileManager.downloadAndValidateFile(onServerUrl, AppProperties.TEMP_DIR.getPath());
        assert modpackFile != null;
        modpackManifest = FileManager.openJsonFile(modpackFile.file());
        assert modpackManifest != null;

        // Initialisation du gestionnaire de mods
        mpFilesManager = new MpFilesManager();
        mpFilesManager.loadManifest(modpackManifest);




        // Téléchargement et ouverture du manifeste admin du modpack
        if (!AdminIdentificator.isAdmin()) {
            modpackAdminManifest = null;
            return;
        }

        String adminUrl = AppProperties.MODPACK_DIR_ON_SERVER + "modpack_admin_manifest.json";
        SignedFile adminModpackFile = FileManager.downloadAndValidateFile(adminUrl, AppProperties.TEMP_DIR.getPath());
        assert adminModpackFile != null;
        modpackAdminManifest = FileManager.openJsonFile(adminModpackFile.file());
        assert modpackAdminManifest != null;

        // Ajout des mods admin au gestionnaire de mods
        mpFilesManager.loadManifest(modpackAdminManifest);

    }



    public void setupModpack() {

        Logger.log(Logger.GREEN + Logger.BOLD + "Gestion du Modpack...");
        mpFilesManager.downloadAll();

    }

    public void verifModpack() {

        Logger.log(Logger.GREEN + Logger.BOLD + "Vérification du Modpack...");
        mpFilesManager.checkAll();

    }

    public void selectOptionalMods() {
        mpFilesManager.selectOptionalFile();
    }



}
