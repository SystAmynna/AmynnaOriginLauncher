package old.old.OriginLauncher.setup.modpack;

import com.amynna.old.Tools.AppProperties;
import com.amynna.old.Tools.FileManager;
import com.amynna.old.Tools.Logger;
import com.amynna.old.Tools.SignedFile;

/** La classe {@code ModpackHandler} gère les opérations liées aux modpacks dans le lanceur. */
public class ModpackHandler {

    /** Gestionnaire des fichiers du modpack */
    private MpFilesManager mpFilesManager;


    public ModpackHandler() {

        // Téléchargement et ouverture du manifeste du modpack
        String onServerUrl = AppProperties.MODPACK_DIR_ON_SERVER + "modpack_manifest.db";
        SignedFile modpackFile = FileManager.downloadAndValidateFile(onServerUrl, AppProperties.TEMP_DIR.getPath());
        assert modpackFile != null; // TODO : gérer les erreurs de téléchargement

        // Initialisation du gestionnaire de mods
        mpFilesManager = new MpFilesManager(modpackFile.file());

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
