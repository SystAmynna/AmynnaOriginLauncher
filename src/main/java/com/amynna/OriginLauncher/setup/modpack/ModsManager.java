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

        // ---[ ATTRIBUTS PRINCIPAUX ]----

        /** Nom du mod. */
        public final String name;
        /** Présence du mod sur le dépôt */
        public final boolean onServer;
        /** Chemin dans le répertoire des mods. */
        public final String path;
        /** URL de téléchargement direct du .jar du mod. */
        private final String url;

        // ---[ ATTRIBUTS SECONDAIRES ]----

        /** Taille du fichier du mod en octets. */
        private final long size;
        /** Hash SHA-512 du fichier du mod pour vérification d'intégrité. */
        private final String sha512;

        // ---[ FICHIERS ]----

        /** Fichier local où le mod sera stocké. */
        public final File file;

        /** Fichier signé si le mod est téléchargé depuis le serveur. */
        private SignedFile signedFile;

        /** Constructeur privé pour initialiser un mod avec ses propriétés. */
        protected Mod(String name, boolean onServer, String path, String url, long size, String sha512) {
            // PRINCIPAUX
            this.name = name;
            this.onServer = onServer;
            this.path = path;

            // SECONDAIRES
            this.url = onServer ? AppProperties.MODS_DIR_ON_SERVER + url : url;
            this.size = size;
            this.sha512 = sha512;

            // FICHIERS
            String pathToDownload = AppProperties.MODS_DIR.getAbsolutePath();
            if (path == null || path.isEmpty()) {
                pathToDownload += File.separator + name;
            } else {
                pathToDownload += File.separator + path;
                if (!path.endsWith(File.separator)) pathToDownload += File.separator;
                pathToDownload += name;
            }
            this.file = new File(pathToDownload);
        }

        /** Télécharge le mod à partir de son URL. */
        protected void download() {
            if (url == null || url.isEmpty()) {
                Logger.error("Impossible de télécharger le mod " + name + " : URL invalide.");
                return;
            }

            if (onServer) signedFile = FileManager.downloadAndValidateFile(url, file.getPath());
            else FileManager.downloadFileAndVerifySha(url, file.getPath(), sha512, FileManager.SHA512);
        }

        /** Vérifie si le fichier existe et si sa taille correspond. */
        protected boolean lightCheck() {
            if (onServer) return signedFile != null && signedFile.exists() && signedFile.valid();
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

        /** Suffixe ajouté au nom du fichier pour indiquer qu'il est désactivé. */
        private static final String DISABLED_TAG = ".disabled";

        /**
         * Constructeur privé pour initialiser un mod avec ses propriétés.
         *
         * @param name     Nom du mod.
         * @param onServer Présence du mod sur le dépôt.
         * @param path     Chemin dans le répertoire des mods.
         * @param url      URL de téléchargement direct du .jar du mod.
         * @param size     Taille du fichier du mod en octets.
         * @param sha512   Hash SHA-512 du fichier du mod pour vérification d'intégrité.
         */
        private OptionalMod(String name, boolean onServer, String path, String url, long size, String sha512) {
            super(name, onServer, path, url, size, sha512);
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

            String path;
            try {path = modJson.getString("path");}
            catch (JSONException e) {path = "";}

            String url;
            long size;
            String sha512;
            boolean onServer;
            try {
                url = modJson.getString("url");
                size = modJson.getLong("size");
                sha512 = modJson.getString("sha512");
                onServer = false;
            }
            catch (JSONException e) {
                url = path;
                if (!url.isEmpty() && !url.endsWith("/")) url += "/";
                size = 0;
                sha512 = "";
                onServer = true;
            }

            // Récupération du nom du mod
            String name;
            if (onServer) {
                try {
                    name = modJson.getString("name");
                } catch (JSONException e) {
                    Logger.error("Mod JSON invalide : " + modJson);
                    continue;
                }
            } else {
                // récupérer le nom à partir de l'URL
                String [] parts = url.split("/");
                name = parts[parts.length - 1];
            }
            // vérifier que le nom finit bien par .jar
            if (!name.endsWith(".jar")) name += ".jar";


            if (onServer) url += name;

            Mod mod = new Mod(
                    name,
                    onServer,
                    path,
                    url,
                    size,
                    sha512
            );
            modsList.add(mod);
        }

        // Retourne la liste des mods
        return modsList;
    }



}
