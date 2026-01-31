package com.amynna.OriginLauncher;

import com.amynna.Tools.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * La classe {@code Auth} gère l'authentification avec Mojang / Microsoft.
 */
public final class Auth {

    private HttpClient httpClient = null;

    private JavaAuthManager.Builder authManagerBuilder = null;

    private JavaAuthManager authManager = null;

    private static final String BASE_CLIENT_ID = "000000004C12AE29";


    public Auth() {
        String userAgent = AppProperties.APP_NAME + "/" + AppProperties.APP_VERSION +
                " (" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "; " +
                System.getProperty("os.arch") + ") Java/" + System.getProperty("java.version");
        httpClient = MinecraftAuth.createHttpClient(userAgent);

        authManagerBuilder = JavaAuthManager.create(httpClient);



    }

    public void authentifie() {

        tryRestaureToken();

        try {

            if (isAuthenticated()) {

                System.out.println("✅ Authentifié en tant que " + authManager.getMinecraftProfile().getUpToDate().getName());

            } else {

                authManager = authManagerBuilder.login(DeviceCodeMsaAuthService::new, new Consumer<MsaDeviceCode>() {
                    @Override
                    public void accept(MsaDeviceCode deviceCode) {
                        // Method to generate a verification URL and a code for the user to enter on that page
                        //System.out.println("Go to " + deviceCode.getVerificationUri());
                        //System.out.println("Enter code " + deviceCode.getUserCode());

                        // There is also a method to generate a direct URL without needing the user to enter a code
                        //System.out.println("Go to " + deviceCode.getDirectVerificationUri());
                        Asker.openUrlInBrowser(deviceCode.getDirectVerificationUri());
                    }
                });
                Logger.log("Username: " + authManager.getMinecraftProfile().getUpToDate().getName());
                Logger.log("Access token: " + authManager.getMinecraftToken().getUpToDate().getToken());

            }

        } catch (Exception e) {
            Logger.fatal("Erreur lors de la tentative de restauration du jeton : " + e.getMessage(), 0);
        }

        saveToken();
    }

    private void saveToken() {
        JsonObject serializedAuthManager = JavaAuthManager.toJson(authManager);
        String jsonString = serializedAuthManager.toString();

        String password = getTokenPwd();
        String alias = AppProperties.MS_TOKEN_ALIAS;

        Encrypter.saveToken(alias, jsonString, password);

    }

    private void tryRestaureToken() {


        String password = getTokenPwd();
        String alias = AppProperties.MS_TOKEN_ALIAS;

        String jsonTokenString = Encrypter.loadToken(alias, password);
        if (jsonTokenString == null) return;
        JsonObject serializedAuthManager = JsonParser.parseString(jsonTokenString).getAsJsonObject();

        try {
            authManager = JavaAuthManager.fromJson(httpClient, serializedAuthManager);
        } catch (NoSuchElementException e) {
            Logger.error("❌ Échec de la restauration du jeton de rafraîchissement (probablement obsolète).");
            try {
                FileManager.deleteFileIfExists(AppProperties.MS_AUTH_TOKEN);
            } catch (SecurityException ex) {
                Logger.error("Impossible de supprimer le fichier de jeton obsolète.");
            }
        }
    }

    public boolean isAuthenticated() {
        return authManager != null && authManager.getMinecraftToken() != null;
    }

    public String getUsername() {
        if (!isAuthenticated()) return "";
        try {
            return authManager.getMinecraftProfile().getUpToDate().getName();
        } catch (IOException e) {
            return "";
        }
    }

    public String getAccessToken() {
        if (!isAuthenticated()) return "";
        try {
            return authManager.getMinecraftToken().getUpToDate().getToken();
        } catch (IOException e) {
            return "";
        }
    }

    public String getUUID() {
        if (!isAuthenticated()) return "";
        try {
            return authManager.getMinecraftProfile().getUpToDate().getId().toString();
        } catch (IOException e) {
            return "";
        }
    }

    public String getClientId() {
        // Je ne sais pas pourquoi mais ça fonctionne même avec une valeur statique
        // TODO : Corriger cela pour utiliser un vrai client ID
        return AppProperties.APP_NAME;
    }

    public String getXuid() {
        // Je ne sais pas pourquoi mais ça fonctionne même avec une valeur statique
        if (!isAuthenticated()) return "";
        return "xuid";

        /*
        try {
            // Le XUID est stocké dans les données du jeton Minecraft
            return authManager.getMinecraftToken().getUpToDate().getXuid();

        } catch (IOException e) {
            return "";
        }

         */
    }

    private static String getTokenPwd() {

        String builder = AppProperties.APP_NAME +
                AppProperties.APP_VERSION +
                System.getProperty("user.name") +
                "token" +
                System.getProperty("os.name") +
                System.getProperty("os.version");

        return Encrypter.sha512(builder);
    }

}
