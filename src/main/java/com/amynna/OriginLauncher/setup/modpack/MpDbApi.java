package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.OriginLauncher.AdminIdentificator;
import com.amynna.Tools.Logger;
import com.amynna.Tools.SqLiteApi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MpDbApi {

    private final SqLiteApi db;

    private final String MP_FILES_TABLE = "Files";
    private final String DEPENDENCIES_TABLE = "Dependencies";
    private final String TYPES_TABLE = "ToInstall";


    protected MpDbApi(File dbFile) {

        try {
            db = new SqLiteApi(dbFile);
        } catch (Exception e) {
            Logger.log("Erreur lors de l'ouverture de la base de données du modpack : " + e.getMessage());
            throw new RuntimeException(e);
        }

    }


    protected List<MpFile> extractAllMpFiles() {

        String query = "SELECT path, url, hash, size, name, description FROM " + MP_FILES_TABLE;

        List<Map<String, String>> results = db.selectToMapList(query);

        List<MpFile> mpFiles = new ArrayList<>();

        for (Map<String, String> row : results) {
            String path = row.get("path");
            String url = row.get("url");
            String hash = row.get("hash");
            long size = Long.parseLong(row.get("size"));
            String name = row.get("name");
            String description = row.get("description");

            MpFile mpFile = new MpFile(path, url, hash, size);
            mpFile.setMetaData(name, description);

            mpFiles.add(mpFile);
        }

        return mpFiles;

    }

    protected void extractDependencies(List<MpFile> mpFiles) {

        String query = "SELECT file, dependency FROM " + DEPENDENCIES_TABLE;

        List<Map<String, String>> results = db.selectToMapList(query);

        for (Map<String, String> row : results) {
            String filePath = row.get("file");
            String dependencyPath = row.get("dependency");

            MpFile file = mpFiles.stream()
                    .filter(f -> f.path.equals(filePath))
                    .findFirst()
                    .orElse(null);
            if (file == null) {
                Logger.log("Fichier référencé dans les dépendances mais non trouvé dans la table des fichiers : " + filePath);
                continue;
            }

            file.addDependency(dependencyPath);
        }


    }

    protected void excludeAdminFiles(List<MpFile> mpFiles) {

        String query = "SELECT path FROM " + TYPES_TABLE + " WHERE admin_required = 1";

        List<Map<String, String>> results = db.selectToMapList(query);

        for (Map<String, String> row : results) {
            String path = row.get("path");

            MpFile file = mpFiles.stream()
                    .filter(f -> f.path.equals(path))
                    .findFirst()
                    .orElse(null);
            if (file == null) {
                Logger.log("Fichier référencé comme admin_required mais non trouvé dans la table des fichiers : " + path);
                continue;
            }

            mpFiles.remove(file);
        }
    }

    protected void replaceWithOptionalFiles(List<MpFile> mpFiles) {

        String query = "SELECT path, admin_required FROM " + TYPES_TABLE + " WHERE optional = 1";

        List<Map<String, String>> results = db.selectToMapList(query);

        for (Map<String, String> row : results) {
            String path = row.get("path");
            boolean isAdminRequired = "1".equals(row.get("admin_required"));

            if (isAdminRequired && !AdminIdentificator.isAdmin()) {
                continue;
            }

            MpFile file = mpFiles.stream()
                    .filter(f -> f.path.equals(path))
                    .findFirst()
                    .orElse(null);
            if (file == null) {
                Logger.log("Fichier référencé dans les types mais non trouvé dans la table des fichiers : " + path);
                continue;
            }

            OptionalMpFile optionalFile = file.convertToOptional();
            mpFiles.set(mpFiles.indexOf(file), optionalFile);

        }


    }

    protected List<MpFile> getFilesToDownload(List<MpFile> mpFiles) {

        String query = "SELECT path FROM " + TYPES_TABLE;

        List<Map<String, String>> results = db.selectToMapList(query);

        List<MpFile> toDownload = new ArrayList<>();

        for (Map<String, String> row : results) {
            String path = row.get("path");

            MpFile file = mpFiles.stream()
                    .filter(f -> f.path.equals(path))
                    .findFirst()
                    .orElse(null);
            if (file == null) {
                Logger.log("Fichier référencé dans les types mais non trouvé dans la table des fichiers : " + path);
                continue;
            }

            toDownload.add(file);
        }

        return toDownload;

    }

}
