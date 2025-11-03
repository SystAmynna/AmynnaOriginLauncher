package com.amynna.OriginLauncher;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Asker;
import com.amynna.Tools.Encrypter;
import com.amynna.Tools.Logger;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;

/**
 * La classe {@code Auth} gère l'authentification avec Microsoft.
 */
public final class Auth {

    /**
     * Le jeton de rafraîchissement utilisé pour maintenir la session active.
     */
    private String token;
    /**
     * Le résultat de l'authentification avec Microsoft.
     */
    private MicrosoftAuthResult msAuthResult;

    private static final String PROFILE_API_URL = "https://api.minecraftservices.com/minecraft/profile";

    /**
     * Méthode principale qui gère l'authentification avec Microsoft.
     */
    public void authentifie() {

        // gestion des jetons de rafraîchissement
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        msAuthResult = null;

        try {

            if (haveSavedToken() && restaureToken()) { // Si un jeton de rafraîchissement est sauvegardé
                // restaure le jeton de rafraîchissement sauvegardé
                msAuthResult = authenticator.loginWithRefreshToken(token);
            } else {
                // demande les identifiants à l'utilisateur
                /*
                String[] credentials = Asker.askAuthentication();
                if (credentials == null) {
                    Logger.log("Authentification annulée par l'utilisateur.");
                    return;
                }
                String email = credentials[0];
                String password = credentials[1];


                // effectue la connexion avec les identifiants
                msAuthResult = authenticator.loginWithCredentials(email, password);

                 */
                // Procède avec JavaFX WebView
                try {
                    msAuthResult = authenticator.loginWithWebview();
                } catch (NullPointerException e) {
                    Logger.error("Échec de l'authentification via WebView : " + e.getMessage());
                    return;
                }


                // sauvegarde le jeton de rafraîchissement
                saveToken(msAuthResult.getRefreshToken());
            }
        } catch (MicrosoftAuthenticationException e) {
            Logger.error("Erreur d'authentification aux services de Microsoft : " + e.getMessage());
        }

        if (msAuthResult == null) {
            Logger.fatal("Échec de l'authentification.");
            return;
        }

        Logger.log("Connecté en tant que " + msAuthResult.getProfile().getName() + " (UUID : " + msAuthResult.getProfile().getId() + ")");

    }


    /**
     * Récupère le XUID (Xbox User ID) et met à jour l'objet MicrosoftAuthResult.
     * Cette méthode interroge l'API de profil Minecraft en utilisant le jeton d'accès
     * déjà contenu dans l'objet d'authentification.
     * * @param authResult L'objet MicrosoftAuthResult contenant le jeton d'accès Minecraft.
     * @throws Exception Si une erreur de connexion ou de lecture se produit.
     */
    public static String getXUID(MicrosoftAuthResult authResult) {
        String accessToken = authResult.getAccessToken();

        String xuid = "0"; // Valeur par défaut en cas d'échec

        if (accessToken == null || accessToken.isEmpty()) {
            return xuid;
        }


        HttpURLConnection conn = null;
        try {
            URL url = new URL(PROFILE_API_URL);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            // Utiliser le jeton d'accès Minecraft comme Bearer Token
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Logger.error("Erreur lors de la récupération du profil. Code de réponse: " + responseCode);
                // Si l'API échoue, nous conservons le XUID à "0"
                return xuid;
            }

            // Lecture de la réponse
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                JSONObject profileJson = new JSONObject(content.toString());

                // Le XUID est généralement une propriété dans le tableau 'properties'
                if (profileJson.has("properties")) {
                    JSONArray properties = profileJson.getJSONArray("properties");
                    for (int i = 0; i < properties.length(); i++) {
                        JSONObject prop = properties.getJSONObject(i);
                        if ("xuid".equals(prop.getString("name"))) {
                            xuid = prop.getString("value");
                            System.out.println("XUID récupéré avec succès: " + xuid);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Erreur réseau ou de parsing lors de la récupération du XUID: " + e.getMessage());
            // Propagation de l'erreur pour la gestion par l'appelant
        } finally {
            conn.disconnect();
            return xuid;
        }
    }

    public String getXUID() {
        return getXUID(msAuthResult);
    }


    /**
     * Vérifie si un jeton de rafraîchissement est déjà sauvegardé.
     *
     * @return true si un jeton est trouvé, false sinon.
     */
    private boolean haveSavedToken() {

        File tokenFile = AppProperties.MS_AUTH_TOKEN;

        if (!tokenFile.exists()) return false;

        if (!tokenFile.isFile() || !tokenFile.canRead()) {
            Logger.error("Le fichier de jeton de rafraîchissement n'est pas lisible.");
            try {
                tokenFile.delete();
            } catch (Exception e) {
                Logger.error("Impossible de supprimer le fichier de jeton corrompu.");
            }
            return false;
        }

        return true;

    }

    /**
     * Restaure le jeton de rafraîchissement depuis un fichier.
     *
     * @return true si la restauration a réussi, false sinon.
     */
    private boolean restaureToken() {
        String password = getTokenPwd();
        String alias = AppProperties.MS_TOKEN_ALIAS;

        token = Encrypter.loadToken(alias, password);
        boolean result = token != null;

        if (result) {
            Logger.log("🔐 Jeton de rafraîchissement restauré.");
        } else {
            Logger.log("❌ Échec de la restauration du jeton de rafraîchissement (probablement obselète).");
            try {
                // TODO: décommenter la ligne suivante une fois le KeyStore géré correctement
                //AppProperties.MS_AUTH_TOKEN.delete();
            } catch (Exception e) {
                Logger.error("Impossible de supprimer le fichier de jeton obselète.");
            }
        }
        return result;
    }

    /**
     * Sauvegarde le jeton de rafraîchissement dans un fichier.
     *
     * @param token Le jeton de rafraîchissement à sauvegarder.
     */
    private void saveToken(String token) {
        String password = getTokenPwd();
        String alias = AppProperties.MS_TOKEN_ALIAS;

        Encrypter.saveToken(alias, token, password);
    }

    /**
     * Obtient le résultat de l'authentification avec Microsoft.
     *
     * @return Le résultat de l'authentification.
     */
    public MicrosoftAuthResult getMsAuthResult() {
        return msAuthResult;
    }

    /**
     * Génère un mot de passe pour le stockage du jeton de rafraîchissement.
     * Le mot de passe est basé sur des informations spécifiques à l'application et au système.
     * (change tous les mois)
     *
     * @return Le mot de passe généré.
     */
    private String getTokenPwd() {
        StringBuilder builder = new StringBuilder();

        builder.append(AppProperties.APP_NAME);

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();
        builder.append(year*12 + month);

        builder.append(AppProperties.APP_VERSION);

        builder.append(System.getProperty("user.name"));

        builder.append("token");

        builder.append(System.getProperty("os.name"));
        builder.append(System.getProperty("os.version"));

        //return Encrypter.sha512(builder.toString());
        return "tmpPwd123"; // Temporary fixed password for testing
    }


    public boolean isAuthenticated() {
        return msAuthResult != null;
    }

}
