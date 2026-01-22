package com.amynna.OriginBootstrap;

import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ManifestGenerator {

    private final File baseDir;
    private final File existingManifestFile;
    private final JSONObject existingManifest;

    private class FileEntry {
        protected String path;
        protected String url;
        protected long size;
        protected String sha512;
        protected String name;
        protected String description;
        protected FileEntry [] subFiles;

        public FileEntry(String path) {
            this.path = path;
        }

        public FileEntry(JSONObject jsonObject) {
            this.path = jsonObject.optString("path", null);
            this.url = jsonObject.optString("url", null);
            this.size = jsonObject.optLong("size", 0);
            this.sha512 = jsonObject.optString("sha512", null);
            this.name = jsonObject.optString("name", null);
            this.description = jsonObject.optString("description", null);

            JSONArray subFilesArray = jsonObject.optJSONArray("sub_files");
            if (subFilesArray != null) {
                this.subFiles = new FileEntry[subFilesArray.length()];
                for (int i = 0; i < subFilesArray.length(); i++) {
                    this.subFiles[i] = new FileEntry(subFilesArray.getJSONObject(i));
                }
            } else {
                this.subFiles = new FileEntry[0];
            }
        }

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("path", this.path);
            if (this.url != null) jsonObject.put("url", this.url);
            if (this.size > 0) jsonObject.put("size", this.size);
            if (this.sha512 != null) jsonObject.put("sha512", this.sha512);
            if (this.name != null) jsonObject.put("name", this.name);
            if (this.description != null) jsonObject.put("description", this.description);

            if (this.subFiles != null && this.subFiles.length > 0) {
                JSONArray subFilesArray = new JSONArray();
                for (FileEntry subFile : this.subFiles) {
                    subFilesArray.put(subFile.toJson());
                }
                jsonObject.put("sub_files", subFilesArray);
            }

            return jsonObject;
        }

    }




    protected ManifestGenerator(File baseDir, File existingManifestFile) {

        this.baseDir = baseDir;
        this.existingManifestFile = existingManifestFile;
        this.existingManifest = FileManager.openJsonFile(existingManifestFile);

    }

    protected File generateManifestFile() {

        // Extraire le main du manifeste existant s'il y en a un
        JSONArray existingMainManifestArray = null;
        if (this.existingManifest != null) {
            existingMainManifestArray = this.existingManifest.optJSONArray("main");
        }
        if (existingMainManifestArray == null) {
            existingMainManifestArray = new JSONArray();
        }

        // Convertir en liste de FileEntry
        List<FileEntry> entries = new ArrayList<>();
        for (Object obj : existingMainManifestArray) {
            if (obj instanceof JSONObject jsonObject) {
                entries.add(new FileEntry(jsonObject));
            }
        }

        // Extraire l'optional du manifeste existant s'il y en a un
        JSONArray existingOptionalManifestArray = null;
        if (this.existingManifest != null) {
            existingOptionalManifestArray = this.existingManifest.optJSONArray("optional");
        }
        if (existingOptionalManifestArray == null) {
            existingOptionalManifestArray = new JSONArray();
        }

        // Convertir en liste de FileEntry
        List<FileEntry> optionalEntries = new ArrayList<>();
        for (Object obj : existingOptionalManifestArray) {
            if (obj instanceof JSONObject jsonObject) {
                optionalEntries.add(new FileEntry(jsonObject));
            }
        }

        // Générer la liste des chemins de fichiers dans le répertoire de base
        List<String> paths = genPathList(this.baseDir, "", true);

        // Supprimer les chemins déjà présents dans le manifeste existant
        for (FileEntry entry : entries) {
            removeIfContains(entry, paths);
        }
        for (FileEntry entry : optionalEntries) {
            removeIfContains(entry, paths);
        }

        // Créer le nouveau manifeste JSON
        JSONArray newMainManifest = new JSONArray();
        for (FileEntry entry : entries) {
            newMainManifest.put(entry.toJson());
        }

        // Ajouter les nouveaux chemins restants
        putAllPathsInJSONArray(newMainManifest, paths);

        JSONArray newOptionalManifest = new JSONArray();
        for (FileEntry entry : optionalEntries) {
            newOptionalManifest.put(entry.toJson());
        }

        JSONObject newManifest = new JSONObject();
        newManifest.put("main", newMainManifest);
        newManifest.put("optional", newOptionalManifest);

        // Enregistrer le nouveau manifeste dans un fichier
        return FileManager.saveJsontoFile(newManifest, existingManifestFile);

    }

    private File formatFile(File file) {
        String name = file.getName();
        String formattedName = name.replace(" ", "_").replace("'", "_");

        if (formattedName.equals(name)) {
            return file;
        }

        File formattedFile = new File(file.getParentFile(), formattedName);
        if (file.renameTo(formattedFile)) {
            return formattedFile;
        }

        // Si le renommage échoue, retourner le fichier original
        Logger.log("Impossible de renommer : " + file.getAbsolutePath());
        return file;
    }

    private List<String> genPathList(File file, String path, boolean isRoot) {

        List<String> paths = new ArrayList<>();

        if (!file.exists() || !file.isDirectory()) {
            return paths;
        }

        File [] subFiles = file.listFiles();
        if (subFiles == null || subFiles.length == 0) {
            return paths;
        }

        if (!isRoot) path += file.getName() + "/";

        for (File subFile : subFiles) {
            if (subFile.isDirectory()) {
                paths.addAll(genPathList(subFile, path, false));
            } else {
                File formattedFile = formatFile(subFile);
                paths.add(path + formattedFile.getName());
            }
        }
        return paths;

    }

    private void removeIfContains(FileEntry entry, List<String> paths) {
        // Supprimer instance path
        paths.remove(entry.path);

        // Supprimer url path
        if (entry.url != null) {
            String [] urlParts = entry.url.split("/");
            String name = urlParts[urlParts.length - 1];
            paths.removeIf(path -> path.endsWith(name));
        }

        // Supprimer subFiles paths
        if (entry.subFiles != null) {
            for (FileEntry subFile : entry.subFiles) {
                removeIfContains(subFile, paths);
            }
        }
    }

    private void putAllPathsInJSONArray(JSONArray jsonArray, List<String> paths) {
        for (String path : paths) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("path", path);
            jsonArray.put(jsonObject);
        }
    }








}
