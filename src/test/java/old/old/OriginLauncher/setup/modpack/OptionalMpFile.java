package old.old.OriginLauncher.setup.modpack;

import com.amynna.old.Tools.FileManager;
import com.amynna.old.Tools.Logger;

import java.io.File;

public class OptionalMpFile extends MpFile {

    /** Suffixe ajouté au nom du fichier pour indiquer qu'il est désactivé. */
    private static final String DISABLED_TAG = ".disabled";

    /** Indique si le mod optionnel est activé ou non. */
    private boolean enabled;

    private final File disabledFile;

    protected OptionalMpFile(String path) {
        super(path);
        this.disabledFile = new File(file.getAbsolutePath() + DISABLED_TAG);
        this.enabled = file.exists();
    }

    protected OptionalMpFile(String path, String url, long size, String sha512) {
        super(path, url, sha512, size);
        this.disabledFile = new File(file.getAbsolutePath() + DISABLED_TAG);
        this.enabled = file.exists();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() {
        if (enabled) return;
        Logger.log("Activation du mod optionnel : " + getName());
        this.enabled = true;
        // vérifie si le mod est présent
        if (disabledFile.exists()) FileManager.renameFile(disabledFile, file);

        // l'installe si nécessaire
        if(!check()) download();
    }

    public void disable() {
        if (!enabled) return;
        Logger.log("Désactivation du mod optionnel : " + getName());
        this.enabled = false;
        // désactive le mod en le renommant
        if (check()) FileManager.renameFile(file, disabledFile);
        else FileManager.deleteFileIfExists(file);
    }

}
