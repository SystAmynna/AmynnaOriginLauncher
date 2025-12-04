package com.amynna.OriginLauncher.setup.modpack;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ModrinthAPI {

    private static final String API_BASE_URL = "https://api.modrinth.com/v2";

    /**
     * Récupère l'URL de téléchargement direct (.jar) d'un mod spécifique à partir de son nom, version et modloader.
     * @param name Le nom du mod (ex: "Roughly Enough Items").
     * @param version La version exacte du mod (ex: "12.0.658").
     * @param modloader Le modloader utilisé (ex: "fabric", "forge").
     * @return Un JSONObject contenant l'URL, la taille et le sha512 du fichier, ou null si non trouvé ou erreur.
     */
    public JSONObject getJarDetails(String name, String version, String modloader) {

        String slug = getModSlugByName(name);
        if (slug == null) {
            Logger.error("Aucun slug trouvé pour le mod : " + name);
            return null;
        }

        JSONObject download = getModFileDetails(slug, version, modloader);
        if (download == null) {
            Logger.error("Aucun lien de téléchargement trouvé pour le mod : " + name + " (" + slug + ") : " + version);
            return null;
        }

        return download;

    }

    /**
     * Recherche le slug Modrinth d'un mod à partir de son nom, en utilisant org.json.
     * @param modName Le nom ou une partie du nom du mod à rechercher.
     * @return Le slug du premier résultat trouvé, ou null si non trouvé ou en cas d'erreur.
     */
    private String getModSlugByName(String modName) {
        // Encodage de la requête pour l'URL
        String encodedQuery = modName.replace(" ", "%20");
        // On limite à 1 résultat pour obtenir le plus pertinent rapidement
        String searchUrl = API_BASE_URL + "/search?query=" + encodedQuery + "&limit=1";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            // Envoi de la requête HTTP
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                Logger.error("Erreur API : Statut code " + response.statusCode() + " pour " + modName);
                return null;
            }

            // --- Parsing avec org.json ---
            // Le corps de la réponse est lu par le JSONTokener
            JSONObject root = new JSONObject(new JSONTokener(response.body()));

            // Le résultat de la recherche est dans le tableau "hits"
            JSONArray hits = root.getJSONArray("hits");

            if (!hits.isEmpty()) {
                // On prend le premier résultat
                JSONObject firstHit = hits.getJSONObject(0);
                return firstHit.getString("slug");
            } else return null;


        } catch (IOException | InterruptedException e) {
            Logger.error("Erreur lors de la requête : " + e.getMessage());
            return null;
        } catch (org.json.JSONException e) {
            Logger.error("Erreur de parsing JSON : " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère les détails du fichier de la version de mod spécifiée.
     *
     * @param modSlug    Le slug (ID URL) du mod (ex: "rei").
     * @param modVersion La version exacte du mod (ex: "12.0.658").
     * @param modloader  Le modloader ciblé (ex: "fabric" ou "forge").
     * @return Un JSONObject contenant l'URL, la taille et le sha512 du fichier, ou null si non trouvé ou erreur.
     */
    public JSONObject getModFileDetails(String modSlug, String modVersion, String modloader) {

        // 1. Préparation des paramètres de filtre (format JSON string encodé)

        // Modrinth attend un tableau JSON stringifié pour les filtres
        // NOTE: Remplacer AppProperties.MINECRAFT_VERSION par la variable de votre contexte réel
        String gameVersionsJson = "[\"" + AppProperties.MINECRAFT_VERSION + "\"]";
        String loadersJson = "[\"" + modloader + "\"]";

        // Il est crucial d'encoder les paramètres d'URL (surtout les crochets et guillemets)
        String encodedGameVersions = URLEncoder.encode(gameVersionsJson, StandardCharsets.UTF_8);
        String encodedLoaders = URLEncoder.encode(loadersJson, StandardCharsets.UTF_8);

        // 2. Construction de l'URL de l'API de versions filtrée
        String versionUrl = String.format(
                "%s/project/%s/version?game_versions=%s&loaders=%s",
                API_BASE_URL,
                modSlug,
                encodedGameVersions,
                encodedLoaders
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(versionUrl))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                Logger.error("Erreur API Modrinth lors de la recherche de versions pour " + modSlug + ": Statut code " + response.statusCode());
                return null;
            }

            // 3. Parsing du corps JSON (qui est un JSONArray)
            JSONArray versionsArray = new JSONArray(new JSONTokener(response.body()));

            // --- Début de la modification pour gérer "latest" ---

            // La première version du tableau (indice 0) est la plus récente, car l'API trie par date de publication (desc)
            JSONObject targetVersion = null;

            if (versionsArray.isEmpty()) {
                // Si le tableau est vide après les filtres, il n'y a pas de version
                //Logger.error("Aucune version du mod " + modSlug + " trouvée pour les critères spécifiés.");
                return null;
            }

            // Si l'utilisateur demande "latest", on prend la première version trouvée (la plus récente)
            if (modVersion != null && modVersion.equalsIgnoreCase("lastest")) {
                targetVersion = versionsArray.getJSONObject(0);
            } else {
                // 4. Parcourir les versions filtrées pour trouver la correspondance exacte (version_number)
                for (int i = 0; i < versionsArray.length(); i++) {
                    JSONObject version = versionsArray.getJSONObject(i);

                    // On vérifie le numéro de version du mod
                    if (version.getString("version_number").equalsIgnoreCase(modVersion)) {
                        targetVersion = version;
                        break; // Version trouvée, on arrête la boucle
                    }
                }
            }

            if (targetVersion == null) {
                // Aucune version trouvée après la recherche spécifique ou si "latest" n'a rien donné
                //Logger.log("Aucune version de mod trouvée pour " + modSlug + " (version mod: " + modVersion + ", mc: " + AppProperties.MINECRAFT_VERSION + ", loader: " + modloader + ")");
                return null;
            }

            // --- Extraction des détails de la version cible ---

            // La version cible est trouvée (que ce soit 'latest' ou une version spécifique)
            JSONArray files = targetVersion.getJSONArray("files");

            if (!files.isEmpty()) {
                JSONObject fileDetails = files.getJSONObject(0);

                // --- 5. Création et remplissage du JSONObject de retour ---
                JSONObject result = new JSONObject();

                // a. URL de téléchargement
                result.put("url", fileDetails.getString("url"));

                // b. Taille en octets
                result.put("size", fileDetails.getLong("size"));

                // c. Hachage SHA-512 (dans l'objet "hashes")
                JSONObject hashes = fileDetails.getJSONObject("hashes");
                result.put("sha512", hashes.getString("sha512"));

                return result; // Retourne le JSONObject complet
            } else {
                Logger.error("La version trouvée (" + targetVersion.getString("version_number") + ") ne contient aucun fichier.");
                return null;
            }

        } catch (IOException | InterruptedException e) {
            Logger.error("Erreur lors de la requête HTTP : " + e.getMessage());
            return null;
        } catch (JSONException e) {
            Logger.error("Erreur de parsing JSON : " + e.getMessage());
            return null;
        }
    }

}