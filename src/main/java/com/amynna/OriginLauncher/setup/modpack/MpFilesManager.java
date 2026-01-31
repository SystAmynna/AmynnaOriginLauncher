package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.Tools.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/** La classe {@code MpFilesManager} gère les fichiers du modpack dans le lanceur. */
public class MpFilesManager {

    /** Liste des fichiers du modpack. */
    private List<MpFile> mpFiles;
    /** Liste des fichiers optionnels du modpack. */
    private List<OptionalMpFile> optionalMpFiles;

    /** Représente un fichier du modpack. (un mod, un fichier de config...) */
    private class MpFile {
        public final String path;
        public final File file;
        private boolean signed;

        private String url;
        private long size;
        private String sha512;

        protected String name;
        private String description;

        private MpFile [] subFiles;

        /** Constructeur de la classe MpFile.
         * @param path Le chemin relatif du fichier/mod/répertoire dans le répertoire Minecraft.
         */
        protected MpFile(String path) {
            this.path = path;
            // Assure que le chemin se termine par un séparateur de fichier
            if (!path.endsWith(File.separator)) path += File.separator;
            // Initialise le fichier avec le répertoire Minecraft + le chemin donné
            this.file = new File(AppProperties.MINECRAFT_DIR + File.separator + path);

            String[] pathParts = path.split("[/\\\\]");
            this.name = pathParts[pathParts.length - 1];
            this.description = "";

        }
        /** Constructeur de la classe MpFile avec des métadonnées supplémentaires.
         * @param path Le chemin relatif du fichier/mod/répertoire dans le répertoire Minecraft.
         * @param url L'URL de téléchargement du fichier.
         * @param size La taille du fichier en octets.
         * @param sha512 Le hash SHA-512 du fichier pour la validation.
         */
        protected MpFile(String path, String url, long size, String sha512) {
            this(path);
            this.url = url;
            this.size = size;
            this.sha512 = sha512;
        }

        public String getName() {
            return name;
        }
        public String getDescription() {
            return description;
        }

        protected void setSigned(boolean signed) {
            this.signed = signed;
        }

        /** Définit les métadonnées du fichier.
         * @param name Le nom du fichier/mod.
         * @param description La description du fichier/mod.
         */
        protected void setMetaData(String name, String description) {
            this.name = name;
            this.description = description;
        }

        /** Définit les sous-fichiers ou répertoires contenus dans ce fichier/mod.
         * @param subFiles Un tableau de sous-fichiers.
         */
        protected void setSubFiles(MpFile[] subFiles) {
            this.subFiles = subFiles;
        }

        /** Applique la logique de téléchargement appropriée en fonction de si le fichier est signé. */
        protected void download() {
            if (signed) downloadSignedFile();
            else downloadUnsignedFile();

            if (subFiles != null) for (MpFile subFile : subFiles) {
                subFile.download();
            }

        }

        private void downloadSignedFile() {
            String onServerUrl = AppProperties.MODPACK_DIR_ON_SERVER + this.path;
            FileManager.downloadAndValidateFile(onServerUrl, this.file.getAbsolutePath());
        }

        private void downloadUnsignedFile() {
            FileManager.downloadFileAndVerifySha(url, this.file.getAbsolutePath(), sha512, FileManager.SHA512);
        }


        protected boolean lightCheck() {
            boolean ok = (signed) ? checkSignedFile() : lightCheckUnsignedFile();
            if (subFiles != null) for (MpFile subFile : subFiles) {
                ok = ok && subFile.lightCheck();
            }
            return ok;
        }

        private boolean lightCheckUnsignedFile() {
            return size == file.length();
        }


        /** Applique la logique de vérification appropriée en fonction de si le fichier est signé. */
        protected boolean check() {
            boolean ok = (signed) ? checkSignedFile() : checkUnsignedFile();
            if (subFiles != null) for (MpFile subFile : subFiles) {
                ok = ok && subFile.check();
            }
            return ok;
        }

        private boolean checkSignedFile() {
            File signature = FileManager.searchFileInDirectory(AppProperties.SIGNATURE_DIR, file.getName() + AppProperties.SIGNATURE_FILE_EXTENSION);
            if (signature == null || !signature.exists()) {
                signature = FileManager.downloadSignatureFile(AppProperties.MODPACK_DIR_ON_SERVER + path);
            }
            if (signature == null || !signature.exists()) {
                return false;
            }

            SignedFile signedFile = new SignedFile(file, signature);
            return signedFile.valid();


        }

        private boolean checkUnsignedFile() {
            return sha512.equals(FileManager.calculSHA(this.file, FileManager.SHA512));
        }

        protected void setSubMpFiles(MpFile [] mpFiles) {
            this.subFiles = mpFiles;
        }

        protected OptionalMpFile convertToOptional() {
            OptionalMpFile optionalMpFile = new OptionalMpFile(this.path, this.url, this.size, this.sha512);
            optionalMpFile.setMetaData(this.name, this.description);
            optionalMpFile.setSubMpFiles(this.subFiles);
            optionalMpFile.setSigned(signed);
            return optionalMpFile;
        }

    }

    /** Représente un fichier optionnel du modpack. */
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
            super(path, url, size, sha512);
            this.disabledFile = new File(file.getAbsolutePath() + DISABLED_TAG);
            this.enabled = file.exists();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void enable() {
            if (enabled) return;
            Logger.log("Activation du mod optionnel : " + name);
            this.enabled = true;
            // vérifie si le mod est présent
            if (disabledFile.exists()) FileManager.renameFile(disabledFile, file);

            // l'installe si nécessaire
            if(!check()) download();
        }

        public void disable() {
            if (!enabled) return;
            Logger.log("Désactivation du mod optionnel : " + name);
            this.enabled = false;
            // désactive le mod en le renommant
            if (check()) FileManager.renameFile(file, disabledFile);
            else FileManager.deleteFileIfExists(file);
        }

    }

    /** Constructeur de la classe MpFilesManager. */
    protected MpFilesManager() {
        mpFiles = new LinkedList<>();
        optionalMpFiles = new LinkedList<>();
    }

    /** Charge le manifeste des fichiers du modpack. */
    protected void loadManifest(JSONObject manifest) {


        if (manifest == null || manifest.isEmpty()) return;

        // Récupération des mods principaux et optionnels
        final JSONArray mods = manifest.getJSONArray("main");
        final JSONArray optionalMods = manifest.getJSONArray("optional");

        // Chargement des mods
        this.mpFiles.addAll(getMpFilesFromJson(mods));
        // Chargement des mods optionnels
        this.optionalMpFiles.addAll(getOptionalMpFilesFromJson(optionalMods));
    }

    // ---- [ MÉTHODES PROTÉGÉES ] ----

    protected void downloadAll() {
        // Téléchargement des mods principaux
        for (MpFile mpFile : mpFiles) {
            if (!mpFile.lightCheck()) mpFile.download();
        }
        // Téléchargement des mods optionnels
        for (OptionalMpFile optionalMpFile : optionalMpFiles) {
            if (optionalMpFile.isEnabled() && !optionalMpFile.lightCheck()) optionalMpFile.download();
        }
    }

    protected void checkAll() {
        // Vérification des fichiers obligatoires
        for (MpFile mpFile : mpFiles) {
            Logger.logc("Verrification du fichier obligatoire : " + mpFile.name + "... ");
            if (!mpFile.check()) {
                Logger.log(Logger.RED + "[ÉCHEC]");
                mpFile.download();
            } else Logger.log(Logger.GREEN + "[OK]");
        }

        // Vérification des fichiers optionnels
        for (OptionalMpFile mpFile : optionalMpFiles) {
            if (!mpFile.isEnabled()) continue;
            Logger.logc("Verrification du fichier optionnel : " + mpFile.name + "... ");
            if (!mpFile.check()) {
                Logger.log(Logger.RED + "[ÉCHEC]");
                mpFile.download();
            } else Logger.log(Logger.GREEN + "[OK]");
        }
    }

    protected void selectOptionalFile() {
        Asker.askOptionnalMods(optionalMpFiles);
    }

    // ---- [ MÉTHODES PRIVÉES ] ----

    /** Convertit un tableau JSON de mods en une liste de mods. */
    private List<MpFile> getMpFilesFromJson(JSONArray mpFiles) {

        // Vérification de la nullité
        if (mpFiles == null) return null;

        // Liste des mods
        List<MpFile> mpFilesList = new LinkedList<>();

        // Parcours des mods
        for (Object obj : mpFiles ) {
            JSONObject mpFileJson = (JSONObject) obj;
            MpFile mpFile = parseMpFile(mpFileJson);
            if (mpFile == null) continue;

            mpFilesList.add(mpFile);
        }

        // Retourne la liste des mods
        return mpFilesList;
    }

    private List<OptionalMpFile> getOptionalMpFilesFromJson(JSONArray mpFiles) {

        if (mpFiles == null) return null;

        List<OptionalMpFile> mpFileList = new LinkedList<>();

        for (Object obj : mpFiles) {
            JSONObject mpFileJson = (JSONObject) obj;

            MpFile mpFile = parseMpFile(mpFileJson);
            if (mpFile == null) continue;

            mpFileList.add(mpFile.convertToOptional());

        }

        return mpFileList;
    }

    private MpFile parseMpFile(JSONObject fileJson) {

        MpFile mpFile;

        String path;
        try {path = fileJson.getString("path");}
        catch (JSONException e) {
            Logger.error("Le fichier du modpack est invalide : chemin manquant.");
            return null;
        }

        String url = "";
        long size = 0;
        String sha512 = "";
        boolean signed;
        try {
            // Fichier non signé, avec métadonnées supplémentaires
            url = fileJson.getString("url");
            size = fileJson.getLong("size");
            sha512 = fileJson.getString("sha512");
            signed = false;
        } catch (JSONException e) {
            // Fichier signé, pas de métadonnées supplémentaires
            signed = true;
        }

        if (path.contains("%name%")) {
            String [] urlParts = url.split("/");
            String fileName = urlParts[urlParts.length - 1];
            path = path.replace("%name%", fileName);
        }

        if (signed) mpFile = new MpFile(path);
        else mpFile = new MpFile(path, url, size, sha512);
        mpFile.setSigned(signed);


        String name;
        String description;
        try {
            name = fileJson.getString("name");
            description = fileJson.getString("description");
            mpFile.setMetaData(name, description);
        } catch (JSONException e) {
            // Métadonnées optionnelles absentes, on les ignore
        }

        JSONArray subFilesJson;
        try {
            subFilesJson = fileJson.getJSONArray("sub_files");
            List<MpFile> subFiles = getMpFilesFromJson(subFilesJson);
            MpFile [] subFilesArray = new MpFile[subFiles.size()];
            mpFile.setSubMpFiles(subFiles.toArray(subFilesArray));
        } catch (JSONException e) {
            // Pas de sous-fichiers, on les ignore
        }

        return mpFile;

    }



}
