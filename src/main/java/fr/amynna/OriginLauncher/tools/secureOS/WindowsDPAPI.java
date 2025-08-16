package fr.amynna.OriginLauncher.tools.secureOS;

import com.sun.jna.platform.win32.Crypt32Util;

public class WindowsDPAPI {
    protected static String protect(String data) {
        byte[] dataBytes = stringToBytes(data);
        dataBytes = Crypt32Util.cryptProtectData(dataBytes);
        return bytesToString(dataBytes);
    }

    protected static String unprotect(String encrypted) {
        byte[] dataBytes = stringToBytes(encrypted);
        dataBytes = Crypt32Util.cryptUnprotectData(dataBytes);
        return bytesToString(dataBytes);
    }

    /**
     * Convertit un tableau d'octets en chaîne de caractères
     *
     * @param data Le tableau d'octets à convertir
     * @return La chaîne de caractères correspondante
     */
    private static String bytesToString(byte[] data) {
        if (data == null) {
            return null;
        }
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Convertit une chaîne de caractères en tableau d'octets
     *
     * @param data La chaîne à convertir
     * @return Le tableau d'octets correspondant
     */
    private static byte[] stringToBytes(String data) {
        if (data == null) {
            return null;
        }
        return data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

}
