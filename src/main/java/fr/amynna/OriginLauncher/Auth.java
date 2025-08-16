package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.tools.Asker;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;

import java.io.IOException;

/**
 * La classe {@code Auth} gère l'authentification avec Microsoft.
 */
public class Auth {

    /**
     * Le jeton de rafraîchissement utilisé pour maintenir la session active.
     */
    private String token;
    /**
     * Le chemin du fichier où le jeton de rafraîchissement est sauvegardé.
     */
    private String tokenPath = Proprieties.ROOT_PATH + "/MsAuthToken";

    /**
     * Méthode principale qui gère l'authentification avec Microsoft.
     */
    public void process() {

        // gestion des jetons de rafraîchissement
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        MicrosoftAuthResult result = null;

        try {

            if (haveSavedToken()) { // Si un jeton de rafraîchissement est sauvegardé
                // restaure le jeton de rafraîchissement sauvegardé
                result = authenticator.loginWithRefreshToken(token);
            } else {
                // demande les identifiants à l'utilisateur
                String[] credentials = Asker.askAuthentication();
                if (credentials == null) {
                    Printer.fatalError("Authentification annulée par l'utilisateur.");
                    return;
                }
                String email = credentials[0];
                String password = credentials[1];
                // effectue la connexion avec les identifiants
                result = authenticator.loginWithCredentials(email, password);
                // sauvegarde le jeton de rafraîchissement
                saveToken(result.getRefreshToken());
            }
        } catch (MicrosoftAuthenticationException e) {
            Printer.fatalError("Erreur d'authentification aux services de Microsoft : " + e.getMessage());
        }

        if (result == null) {
            Printer.fatalError("Échec de l'authentification.");
            return;
        }

        Printer.printInfo("Connecté en tant que " + result.getProfile().getName() + " (UUID : " + result.getProfile().getId() + ")");

    }

    /**
     * Vérifie si un jeton de rafraîchissement est déjà sauvegardé.
     *
     * @return true si un jeton est trouvé, false sinon.
     */
    private boolean haveSavedToken() {
        try {
            token = FileManager.loadBinary(tokenPath);
        } catch (IOException e) {
            Printer.printError("Erreur lors du chargement du jeton de rafraîchissement : " + e.getMessage());
            return false;
        }
        return token != null && !token.isEmpty();
    }

    /**
     * Sauvegarde le jeton de rafraîchissement dans un fichier.
     *
     * @param token Le jeton de rafraîchissement à sauvegarder.
     */
    private void saveToken(String token) {
        try {
            FileManager.saveBinary(token, tokenPath);
        } catch (IOException e) {
            Printer.printError("Erreur lors de la sauvegarde du jeton de rafraîchissement : " + e.getMessage());
        }
    }

}
