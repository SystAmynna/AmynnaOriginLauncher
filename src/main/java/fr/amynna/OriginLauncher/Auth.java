package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.tools.Asker;
import fr.amynna.OriginLauncher.tools.Config;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;
import fr.amynna.OriginLauncher.tools.secureOS.SecureOS;
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
     * Le résultat de l'authentification avec Microsoft.
     */
    private MicrosoftAuthResult msAuthResult;

    /**
     * Méthode principale qui gère l'authentification avec Microsoft.
     */
    public void process() {

        // gestion des jetons de rafraîchissement
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        msAuthResult = null;

        try {

            if (haveSavedToken()) { // Si un jeton de rafraîchissement est sauvegardé
                // restaure le jeton de rafraîchissement sauvegardé
                msAuthResult = authenticator.loginWithRefreshToken(token);
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
                msAuthResult = authenticator.loginWithCredentials(email, password);
                // sauvegarde le jeton de rafraîchissement
                saveToken(msAuthResult.getRefreshToken());
            }
        } catch (MicrosoftAuthenticationException e) {
            Printer.fatalError("Erreur d'authentification aux services de Microsoft : " + e.getMessage());
        }

        if (msAuthResult == null) {
            Printer.fatalError("Échec de l'authentification.");
            return;
        }

        Printer.printInfo("Connecté en tant que " + msAuthResult.getProfile().getName() + " (UUID : " + msAuthResult.getProfile().getId() + ")");

    }

    /**
     * Vérifie si un jeton de rafraîchissement est déjà sauvegardé.
     *
     * @return true si un jeton est trouvé, false sinon.
     */
    private boolean haveSavedToken() {
        try {
            token = FileManager.loadBinary(tokenPath);


            // décrypte le jeton de rafraîchissement
            if (Config.isTokenSecure()) {
                token = SecureOS.unSecure(token);
            }


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

            if (Config.isTokenSecure()) {
                token = SecureOS.secure(token);
                if (token == null) {
                        if (Proprieties.getOS() != Proprieties.OS.LINUX || Proprieties.getOS() != Proprieties.OS.MACOS) {
                        Printer.printError("Échec de la sécurisation du jeton de rafraîchissement (si vous souhaitez tout de même rester authentifier, désactivez la sécurité d'Authentification Microsoft dans les paramètres du launcher).");
                    }
                    return;
                }
            }
            FileManager.saveBinary(token, tokenPath);
        } catch (IOException e) {
            Printer.printError("Erreur lors de la sauvegarde du jeton de rafraîchissement : " + e.getMessage());
        }
    }

}
