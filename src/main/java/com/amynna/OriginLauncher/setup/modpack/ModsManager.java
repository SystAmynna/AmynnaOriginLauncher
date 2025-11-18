package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import com.amynna.Tools.SignedFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/** La classe {@code ModsManager} gère les mods du modpack dans le lanceur. */
public class ModsManager {

    // ----[ ATTRIBUTS ]----

    /** Classe interne représentant un mod individuel. */
    private static class Mod {

        /** Instance de l'API Modrinth pour récupérer les URLs de téléchargement. */
        private static final ModrinthAPI modrinthAPI = new ModrinthAPI();

        public final String name;
        public final String version;
        public final boolean onServer;
        public final String modloader;

        /** URL de téléchargement direct du .jar du mod. */
        private String url;
        /** Taille du fichier du mod en octets. */
        private long size;
        /** Hash SHA-512 du fichier du mod pour vérification d'intégrité. */
        private String sha512;

        private SignedFile signedFile;

        /** Fichier local où le mod sera stocké. */
        public final File file;

        /** Constructeur privé pour initialiser un mod avec ses propriétés. */
        protected Mod(String name, String version, boolean onServer, String path, String modloader) {
            this.name = name;
            this.version = version;
            this.onServer = onServer;
            this.modloader = modloader;

            final String dlName = name + "-" + version + ".jar";
            String pathToDownload = AppProperties.MODS_DIR.getAbsolutePath();
            if (path == null || path.isEmpty()) {
                pathToDownload += File.separator + dlName;
            } else {
                pathToDownload += File.separator + path;
                if (!path.endsWith(File.separator)) pathToDownload += File.separator;
                pathToDownload += dlName;
            }
            this.file = new File(pathToDownload);

        }

        protected void setDetails() {

            if (onServer) {
                url = AppProperties.MODS_DIR_ON_SERVER + file.getName();
                size = file.length();
                sha512 = null; // La vérification devra être faite via le serveur
                return;
            }

            JSONObject details = modrinthAPI.getJarDetails(name, version, modloader);
            if (details != null) {
                this.url = details.getString("url");
                this.size = details.getLong("size");
                this.sha512 = details.getString("sha512");
                return;
            }


            // Si aucun détail n'a pu être récupéré, initialise avec des valeurs par défaut
            this.url = null;
            this.size = 0;
            this.sha512 = null;
            //Logger.error("Impossible de récupérer les détails du mod : " + name + " version " + version);
        }



        /** Télécharge le mod à partir de son URL. */
        protected void download() {
            if (url == null || url.isEmpty()) {
                Logger.error("Impossible de télécharger le mod " + name + " : URL invalide.");
                return;
            }

            if (onServer) {
                signedFile = FileManager.downloadAndValidateFile(url, file.getPath());
            }
            else FileManager.downloadFileAndVerifySha(url, file.getPath(), sha512, FileManager.SHA512);
        }

        /** Vérifie si le fichier existe et si sa taille correspond. */
        protected boolean lightCheck() {
            if (onServer) return signedFile != null && signedFile.valid();
            return file != null && file.exists() && file.length() == size;
        }

        /** Vérifie l'intégrité du mod en comparant le SHA-512. */
        protected boolean check() {
            if (onServer) return lightCheck();
            if (!lightCheck()) return false;
            String sha512 = FileManager.calculSHA(file, FileManager.SHA512);
            return this.sha512.equals(sha512);
        }

    }

    /** Classe interne représentant un mod optionnel. */
    private static class OptionalMod extends Mod {

        /** Indique si le mod optionnel est activé ou non. */
        private boolean enabled;

        private static final String DISABLED_TAG = ".disabled";

        /**
         * Constructeur privé pour initialiser un mod avec ses propriétés.
         *
         * @param name
         * @param version
         * @param onServer
         * @param path
         * @param modloader
         */
        private OptionalMod(String name, String version, boolean onServer, String path, String modloader) {
            super(name, version, onServer, path, modloader);
            this.enabled = file.exists();
        }

        /** Indique si le mod optionnel est activé. */
        public boolean isEnabled() {
            return enabled;
        }

        /** Active le mod optionnel. */
        public void enable() {
            this.enabled = true;
            // vérifie si le mod est présent
            File disabledFile = new File(file.getPath() + DISABLED_TAG);
            if (disabledFile.exists()) FileManager.renameFile(disabledFile, file);

            // l'installe si nécessaire
            if(!check()) download();
        }

        public void disable() {
            this.enabled = false;
            // désactive le mod en le renommant
            File disabledFile = new File(file.getPath() + DISABLED_TAG);
            if (check()) FileManager.renameFile(file, disabledFile);
            else FileManager.deleteFileIfExists(file);

        }
    }

    /** Liste des mods principaux du modpack. */
    private final List<Mod> mods;

    /** Liste des mods optionnels du modpack. */
    private final List<OptionalMod> optionalMods;

    /** Constructeur protégé pour initialiser le gestionnaire de mods. */
    protected ModsManager() {
        // Initialisation des listes de mods
        mods = new LinkedList<>();
        optionalMods = new LinkedList<>();
    }

    // ---[ MÉTHODES ]----

    /** Charge le manifeste des mods à partir d'un objet JSON. */
    protected void loadManifest(JSONObject manifest) {
        if (manifest == null || manifest.isEmpty()) return;

        // Récupération des mods principaux et optionnels
        final JSONArray mods = manifest.getJSONArray("main");
        final JSONArray optionalMods = manifest.getJSONArray("optional");

        // Chargement des mods
        this.mods.addAll(getModsFromJson(mods));
        // Chargement des mods optionnels
        List<Mod> optMods = getModsFromJson(optionalMods);
        List<OptionalMod> optionalModsList = new LinkedList<>();
        for (Mod mod : optMods) {
            optionalModsList.add((OptionalMod) mod);
        }
        this.optionalMods.addAll(optionalModsList);
    }


    protected void setupMods() {
        for (Mod mod : mods) {
            mod.setDetails();
        }
        for (OptionalMod mod : optionalMods) {
            mod.setDetails();
        }
    }


    protected void downloadAll() {
        // Téléchargement des mods principaux
        for (Mod mod : mods) {
            if (!mod.lightCheck()) mod.download();
        }

        // Téléchargement des mods optionnels activés
        for (OptionalMod mod : optionalMods) {
            if (mod.isEnabled() && !mod.lightCheck()) mod.download();
        }
    }

    protected void checkAll() {
        // Vérification des mods principaux
        for (Mod mod : mods) {
            Logger.logc("Verrification du mod principal : " + mod.name + "... ");
            if (!mod.check()) {
                Logger.log(Logger.RED + "[ÉCHEC]");
                mod.download();
            } else Logger.log(Logger.GREEN + "[OK]");
        }

        // Vérification des mods optionnels activés
        for (OptionalMod mod : optionalMods) {
            Logger.logc("Verrification du mod optionnel : " + mod.name + "... ");
            if (mod.isEnabled() && !mod.check()) {
                Logger.log(Logger.RED + "[ÉCHEC]");
                mod.download();
            } else Logger.log(Logger.GREEN + "[OK]");
        }
    }



    // ----[ MÉTHODES PRIVÉES ]----

    /** Convertit un tableau JSON de mods en une liste de mods. */
    private List<Mod> getModsFromJson(JSONArray mods) {

        // Vérification de la nullité
        if (mods == null) return null;

        // Liste des mods
        List<Mod> modsList = new LinkedList<>();

        // Parcours des mods
        for (Object obj : mods) {
            JSONObject modJson = (JSONObject) obj;

            // Récupération des informations du mod
            final String modName = modJson.getString("name"); //Nom du mod

            // Propriétés optionnelles avec valeurs par défaut
            // Version du mod
            String modVersion;
            try {modVersion = modJson.getString("version");}
            catch (JSONException e) {modVersion = "latest";}
            // onServer (false par défaut)
            boolean modOnServer;
            try {modOnServer = modJson.getBoolean("onServer");}
            catch (JSONException e) {modOnServer = false;}
            // path ("" par défaut)
            String modPath;
            try {modPath = modJson.getString("path");}
            catch (JSONException e) {modPath = "";}
            // modloader (AppProperties.MODLOADER par défaut)
            String modLoader;
            try {modLoader = modJson.getString("modloader");}
            catch (JSONException e) {modLoader = AppProperties.MODLOADER;}

            Mod mod = new Mod(
                    modName,
                    modVersion,
                    modOnServer,
                    modPath,
                    modLoader
            );
            modsList.add(mod);
        }

        // Retourne la liste des mods
        return modsList;
    }








}
