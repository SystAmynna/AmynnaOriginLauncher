package fr.amynna.OriginLauncher.tools.secureOS;

import fr.amynna.OriginLauncher.Proprieties;
import fr.amynna.OriginLauncher.tools.Printer;

public class SecureOS {

    public static String secure(String token) {

        Printer.printDebug("secure");

        if (token == null || token.isEmpty()) {
            return null;
        }

        if (Proprieties.getOS() == Proprieties.OS.LINUX) {
            try {
                LinuxKeyring.store(token);
                return null;
            } catch (Exception e) {
                Printer.printError("Echec de la sauvegarde du token dans le trousseau de clés Linux");
            }
        }
        else if (Proprieties.getOS() == Proprieties.OS.WINDOWS) {
            return WindowsDPAPI.protect(token);
        }
        else if (Proprieties.getOS() == Proprieties.OS.MACOS) {
            try {
                MacKeychain.store(token);
                return null;
            } catch (Exception e) {
                Printer.printError("Echec de la sauvegarde du token dans le trousseau de clés Mac");
            }
        }
        else {
            Printer.printError("Système d'exploitation non supporté pour la sécurisation du token.");
        }
        return null;

    }



    public static String unSecure(String token) {

        Printer.printDebug("unsecure");

        if (Proprieties.getOS() == Proprieties.OS.LINUX) {
            try {
                return LinuxKeyring.load();
            } catch (Exception e) {
                Printer.printWarning("Echec de la récupération du token depuis le trousseau de clés Linux");
            }
        }
        else if (Proprieties.getOS() == Proprieties.OS.WINDOWS) {
            return WindowsDPAPI.unprotect(token);
        }
        else if (Proprieties.getOS() == Proprieties.OS.MACOS) {
            try {
                return MacKeychain.load();
            } catch (Exception e) {
                Printer.printError("Echec de la récupération du token depuis le trousseau de clés Mac");
            }
        }
        else {
            Printer.printError("Système d'exploitation non supporté pour la récupération du token.");
        }
        return null;

    }

}
