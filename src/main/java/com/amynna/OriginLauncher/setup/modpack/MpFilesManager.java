package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.OriginLauncher.AdminIdentificator;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Asker;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;

import java.io.File;
import java.util.List;

public class MpFilesManager {

    private MpDbApi mpDbApi;

    private List<MpFile> mpFiles;

    protected MpFilesManager(File dbFile) {
        mpDbApi = new MpDbApi(dbFile);

        // Récupère la liste de tous les fichiers du modpack à partir de la base de données
        mpFiles = mpDbApi.extractAllMpFiles();

        // Récupérer les dépendances
        mpDbApi.extractDependencies(mpFiles);

        // Exclure les fichiers admin
        if (AdminIdentificator.isAdmin()) {
            Logger.log("Mode admin activé : tous les fichiers du modpack seront inclus.");
        } else {
            mpDbApi.excludeAdminFiles(mpFiles);
        }

        // Remlacer les MpFile par des OptionalMpFile si nécessaire
        mpDbApi.replaceWithOptionalFiles(mpFiles);

    }


    protected void downloadAll() {

        List<MpFile> toDownload = genToDownloadList();

        // Télécharger les fichiers
        Logger.log("Téléchargement de " + toDownload.size() + " fichiers du modpack...");
        for (MpFile mpFile : toDownload) {
            if (!mpFile.lightCheck()) {
                Logger.log("Le fichier" + mpFile.path + " est en cours de téléchargement.");
                mpFile.download();
            }
        }

    }

    protected void checkAll() {

        List<MpFile> toCheck = genToDownloadList();

        // Vérifier les fichiers
        Logger.log("Vérification de " + toCheck.size() + " fichiers du modpack...");
        for (MpFile mpFile : toCheck) {
            if (!mpFile.check()) {
                Logger.log(Logger.RED + "Le fichier " + mpFile.path + " est manquant ou corrompu. Il sera retéléchargé.");
                mpFile.download();
            }
        }

    }

    private List<MpFile> genToDownloadList() {

        List<MpFile> toDownload = mpDbApi.getFilesToDownload(mpFiles);

        // exclure les fichiers optionnels non sélectionnés
        toDownload.removeIf(mpFile -> mpFile instanceof OptionalMpFile optionalMpFile && !optionalMpFile.isEnabled());

        // inclure les dépendances
        for (MpFile mpFile : toDownload) {
            List<String> dependencies = mpFile.getDependencies();
            for (String dependencyPath : dependencies) {
                MpFile file = mpFiles.stream()
                        .filter(f -> f.path.equals(dependencyPath))
                        .findFirst()
                        .orElse(null);
                if (file != null && !toDownload.contains(file)) toDownload.add(file);
            }
        }

        // Vérifier les fichiers existants
        File[] files = AppProperties.MINECRAFT_MODS_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                MpFile mpFile = toDownload.stream()
                        .filter(f -> f.path.equals(file.getName()))
                        .findFirst()
                        .orElse(null);
                if (mpFile == null) {
                    FileManager.deleteFileIfExists(file);
                }
            }
        }

        return toDownload;
    }

    protected void selectOptionalFile() {

        List<MpFile> mpFiles = genToDownloadList();

        List<OptionalMpFile> optionalMpFiles = mpFiles.stream()
                .filter(mpFile -> mpFile instanceof OptionalMpFile)
                .map(mpFile -> (OptionalMpFile) mpFile)
                .toList();

        Asker.askOptionnalMods(optionalMpFiles);

    }







}
