package com.amynna.OriginLauncher.setup.modpack;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/** La classe {@code ModConfigManager} gère la configuration des mods pour le modpack. */
public class ModConfigManager {

    private record ModConfig(String fileName, String url) { }

    private class ConfigFile {

        private final String link;
        private final File file;

        protected ConfigFile(String link) {
            this.link = link;
            this.file = new File(AppProperties.);
        }




    }



    private ModConfig parseModConfig(JSONObject json) {

        String path;
        try {path = json.getString("path");
        } catch (JSONException e) {path = "";}

        JSONArray files;
        try {files = json.getJSONArray("files");}
        catch (JSONException e) {
            Logger.error("Le fichier de configuration des mods est invalide.");
            return null;
        }

        List<ConfigFile> configFiles = new LinkedList<ConfigFile>();

        for (int i = 0; i < files.length(); i++) {

            String fileName;
            try {fileName = files.getString(i);}
            catch (JSONException e) {
                Logger.error("Le fichier de configuration des mods est invalide : 'entrys' doit être une liste de chaînes.");
                continue;
            }

            ConfigFile cf = parseConfigFile(path, fileName);
            if (cf == null) {
                Logger.error("Le fichier de configuration des mods est invalide.");
                continue;
            }

            configFiles.add(cf);

        }

        return new ModConfig(
                path,

        );

    }

    private ConfigFile parseConfigFile(String path, String fileName) {

        if (path == null) path = "";
        if (fileName == null || fileName.isEmpty()) return null;

        if (!path.isEmpty() && !path.endsWith(File.separator)) path += File.separator;

        String finalPath = AppProperties.MINECRAFT_CONFIG_DIR.getAbsolutePath() + File.separator + path + fileName;

    }


}
