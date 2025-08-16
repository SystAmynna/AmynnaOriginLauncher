package fr.amynna.OriginLauncher.tools;

import java.nio.file.Path;

public class Config {

    private static boolean msTokenSecure = true;


    public static void setTokenSecure(boolean secure) {
        msTokenSecure = secure;
    }
    public static boolean isTokenSecure() {
        return msTokenSecure;
    }


}
