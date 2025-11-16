package com.amynna.OriginLauncher;

import com.amynna.Tools.*;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;

import java.io.File;
import java.time.LocalDate;

/**
 * La classe {@code Auth} gère l'authentification avec Mojang / Microsoft.
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

    /**
     * Méthode principale qui gère l'authentification avec Mojang / Microsoft.
     */
    public void authentifie() {

        // Instanciation de l'authentificateur Microsoft
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        msAuthResult = null;

        try {
            // Si un jeton de rafraîchissement est sauvegardé
            if (haveSavedToken() && restaureToken()) {
                // restaure le jeton de rafraîchissement sauvegardé
                msAuthResult = authenticator.loginWithRefreshToken(token);
            } else {
                // Procède au login avec JavaFX WebView
                try {
                    msAuthResult = authenticator.loginWithWebview();
                } catch (Exception e) {
                    Logger.error("Échec de l'authentification via WebView : " + e.getMessage());
                    return;
                }

                // sauvegarde le jeton de rafraîchissement
                saveToken(msAuthResult.getRefreshToken());
            }
        } catch (MicrosoftAuthenticationException e) {
            Logger.error("Erreur d'authentification aux services de Microsoft : " + e.getMessage());
            return;
        }

        // Affiche les informations de l'utilisateur connecté
        Logger.log("Connecté en tant que " + msAuthResult.getProfile().getName() + " (UUID : " + msAuthResult.getProfile().getId() + ")");

    }

    /**
     * Vérifie si l'utilisateur est authentifié.
     *
     * @return {@code boolean} true si l'utilisateur est authentifié, false sinon.
     */
    public boolean isAuthenticated() {
        return msAuthResult != null;
    }

    /**
     * Récupère le résultat de l'authentification Microsoft.
     *
     * @return {@code MicrosoftAuthResult} Le résultat de l'authentification.
     */
    public MicrosoftAuthResult getMsAuthResult() {
        return msAuthResult;
    }


    /**
     * Vérifie si un jeton de rafraîchissement est déjà sauvegardé.
     *
     * @return {@code boolean} true si un jeton est trouvé, false sinon.
     */
    private boolean haveSavedToken() {

        // Instancie le fichier de sauvegarde du jeton
        File tokenFile = AppProperties.MS_AUTH_TOKEN;

        // Vérifie l'existence du fichier
        if (!tokenFile.exists()) return false;

        // Vérifie que le fichier est lisible
        if (!tokenFile.isFile() || !tokenFile.canRead()) {
            Logger.error("Le fichier de jeton de rafraîchissement n'est pas lisible, suppression...");
            FileManager.deleteFileIfExists(tokenFile);
            return false;
        }

        // Atteste que le fichier est valide
        return true;

    }

    /**
     * Restaure le jeton de rafraîchissement depuis un fichier.
     *
     * @return {@code boolean} true si la restauration a réussi, false sinon.
     */
    private boolean restaureToken() {
        // Récupère le mot de passe et l'alias pour le déchiffrement
        String password = getTokenPwd();
        // Récupère l'alias du jeton
        String alias = AppProperties.MS_TOKEN_ALIAS;

        // Tente de charger le jeton chiffré
        token = Encrypter.loadToken(alias, password);
        boolean result = token != null;

        // Log le résultat de la restauration
        if (result) {
            Logger.log("🔐 Jeton de rafraîchissement restauré.");
        } else {
            Logger.log("❌ Échec de la restauration du jeton de rafraîchissement (probablement obselète).");
            try {
                FileManager.deleteFileIfExists(AppProperties.MS_AUTH_TOKEN);
            } catch (SecurityException e) {
                Logger.error("Impossible de supprimer le fichier de jeton obselète.");
            }
        }

        // Retourne le résultat de la tentative de restauration
        return result;
    }

    /**
     * Sauvegarde le jeton de rafraîchissement dans un fichier.
     *
     * @param token Le jeton de rafraîchissement à sauvegarder.
     */
    private void saveToken(String token) {
        // Récupère le mot de passe et l'alias pour le chiffrement
        String password = getTokenPwd();
        // Récupère l'alias du jeton
        String alias = AppProperties.MS_TOKEN_ALIAS;

        // Sauvegarde le jeton chiffré
        Encrypter.saveToken(alias, token, password);
    }

    /**
     * Génère un mot de passe pour le stockage du jeton de rafraîchissement.
     * Le mot de passe est basé sur des informations spécifiques à l'application et au système.
     * (Permanant entre les sessions, mais unique pour chaque utilisateur et installation)
     *
     * @return {@code String} Le mot de passe généré.
     */
    private String getTokenPwd() {

        String builder = AppProperties.APP_NAME +
                AppProperties.APP_VERSION +
                System.getProperty("user.name") +
                "token" +
                System.getProperty("os.name") +
                System.getProperty("os.version");

        return Encrypter.sha512(builder);
    }
}
