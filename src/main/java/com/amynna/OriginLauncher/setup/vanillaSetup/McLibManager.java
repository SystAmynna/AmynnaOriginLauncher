package com.amynna.OriginLauncher.setup.vanillaSetup;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe responsable de la gestion des bibliothèques Minecraft.
 */
public class McLibManager {

    // ----[ ATTRIBUTS ]----

    /** Bibliothèques Minecraft au format JSON. */
    private final JSONArray libraries;

    /** Liste des bibliothèques Minecraft. */
    private final List<McLibrary> mcLibraries;


    /**
     * Classe représentant une bibliothèque Minecraft.
     */
    private record McLibrary(String name, String url, String sha1, int size, File file, boolean isNative) {
        /**
         * Constructeur de la classe McLibrary.
         */
        private McLibrary {}

        /**
         * Télécharge la bibliothèque et vérifie son SHA1.
         */
        public void download() {
            FileManager.downloadFileAndVerifySha1(url, file.getPath(), sha1);
        }

        /**
         * Vérifie l'intégrité de la bibliothèque en comparant le SHA1.
         */
        public boolean check() {
            lightCheck();
            String fileSha1 = FileManager.calculSHA1(file);
            assert fileSha1 != null;
            return fileSha1.equals(sha1);
        }

        /**
         * Vérifie si le fichier existe et si sa taille correspond.
         */
        public boolean lightCheck() {
            return file.exists() && file.length() == size;
        }

    }

    /** Constructeur */
    public McLibManager(JSONArray libs) {
        this.libraries = libs;
        this.mcLibraries = new LinkedList<>();

        makeLibList();
    }

    // ----[ MÉTHODES ]----

    /**
     * Remplit la liste des bibliothèques Minecraft à partir du JSON.
     */
    private void makeLibList() {
        // Vide les listes avant de les remplir
        mcLibraries.clear();

        // Parcourt toutes les bibliothèques définies dans le manifest
        for (int i = 0; i < libraries.length(); i++) {
            // Récupère le JSON de la bibliothèque
            JSONObject libJson = libraries.getJSONObject(i);
            // Construit l'objet McLibrary
            McLibrary lib = parseLib(libJson);
            // Si la bibliothèque n'est pas applicable, on l'ignore
            if (lib == null) continue;
            // Ajoute la bibliothèque à la liste
            mcLibraries.add(lib);
        }

    }

    /**
     * Construit un objet McLibrary à partir de sa définition JSON.
     * Gère les règles de l'OS et détermine si la bibliothèque est native.
     *
     * @param lib Le JSONObject d'une seule bibliothèque (ex: un élément du tableau "libraries").
     * @return Un objet McLibrary prêt à être utilisé, ou null si la bibliothèque
     * ne doit pas être téléchargée sur l'OS actuel.
     */
    private McLibrary parseLib(JSONObject lib) {

        // --- 1. Gérer les règles (Rules) ---
        if (lib.has("rules")) {
            boolean allowed = false; // Par défaut, INTERDIT s'il y a des règles
            JSONArray rules = lib.getJSONArray("rules");

            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                String action = rule.getString("action");

                if (action.equals("allow")) {
                    // Si la règle n'a pas de condition OS, elle s'applique partout
                    if (!rule.has("os")) {
                        allowed = true;
                        break;
                    }

                    // Vérifier si la condition OS correspond à notre OS
                    JSONObject osRule = rule.getJSONObject("os");
                    if (osRule.getString("name").equals(AppProperties.getOsType())) {
                        allowed = true;
                        break; // On a trouvé une règle "allow" qui correspond, on s'arrête
                    }
                }
                // (Nous ignorons les règles "deny" car 1.20.1 n'en utilise pas pour les libs)

            }

            if (!allowed) {
                // Aucune règle "allow" n'a correspondu, on ignore cette bibliothèque
                return null;
            }
        }

        // --- 2. La règle est passée (ou il n'y en avait pas) ---
        try {
            JSONObject downloads = lib.getJSONObject("downloads");

            // Dans ce JSON, toutes les infos sont sous "artifact"
            if (!downloads.has("artifact")) {
                return null; // Pas d'artifact, on ne peut rien télécharger
            }

            JSONObject artifact = downloads.getJSONObject("artifact");

            String name = lib.getString("name");
            String url = artifact.getString("url");
            String sha1 = artifact.getString("sha1");
            int size = artifact.getInt("size");
            String path = artifact.getString("path");

            // --- 3. Déterminer si la lib est "native" ---
            // La méthode la plus simple est de regarder le nom (name)
            boolean isNative = name.contains(":natives-");

            // --- 4. Construire l'objet File ---
            // Crée le chemin complet, ex: ".minecraft/libraries/" + "com/google/gson/..."
            File libFile = new File(AppProperties.MINECRAFT_LIB_DIR, path);

            // --- 5. Retourner le nouvel objet ---
            return new McLibrary(name, url, sha1, size, libFile, isNative);

        } catch (Exception e) {
            Logger.error("Erreur de parsing de la bibliothèque: " + lib.optString("name", "N/A"));

            return null;
        }
    }

    /**
     * Détermine le séparateur de chemin de classe (classpath) en fonction de l'OS.
     *
     * @return Le séparateur (':' ou ';').
     */
    public static String getCpSeparator() {
        // En Java, le séparateur de classpath est stocké dans la propriété du système.
        return System.getProperty("path.separator");
    }

    // ----[ MÉTHODES PUBLIQUES ]----

    /** Télécharge toutes les bibliothèques Minecraft. */
    public void downloadAllLibraries() {
        for (McLibrary lib : mcLibraries) {
            if (!lib.lightCheck()) lib.download();
        }
    }

    /** Vérifie l'intégrité de toutes les bibliothèques Minecraft. */
    public void checkAllLibraries() {
        for (McLibrary lib : mcLibraries) {
            if (!lib.check()) {
                Logger.log("La bibliothèque " + lib.name + " est corrompue ou manquante. Téléchargement...");
                lib.download();
            }
        }
    }


    /**
     * Génère la chaîne complète du classpath (chemin de classe) pour l'exécution de Java.
     * Cette chaîne inclut toutes les bibliothèques standard et le JAR client du jeu.
     *
     * @param clientJarFile Le chemin complet (File) vers le fichier client.jar du jeu.
     * @return La chaîne de classpath formatée, séparée par le caractère approprié à l'OS.
     */
    public String generateClasspath(File clientJarFile) {

        // 1. Déterminer le séparateur de classpath
        // ';' pour Windows, ':' pour Linux/macOS
        String separator = getCpSeparator();

        // 2. Filtrer les bibliothèques non-natives et extraire leurs chemins absolus
        String librariesPath = mcLibraries.stream()
                .filter(lib -> !lib.isNative) // On ignore les bibliothèques natives
                .map(lib -> lib.file.getAbsolutePath())
                .collect(Collectors.joining(separator));

        // 3. Ajouter le JAR du jeu au début ou à la fin de la chaîne.
        // La convention habituelle est de le mettre à la fin, après toutes les dépendances.
        String clientPath = clientJarFile.getAbsolutePath();

        if (librariesPath.isEmpty()) {
            return clientPath;
        } else {
            return librariesPath + separator + clientPath;
        }
    }








}
