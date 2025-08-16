package fr.amynna.OriginLauncher.tools;

public class Encrypt {

    public static String encrypt(String word, String key) {
        StringBuilder encrypted = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = key.charAt(i % key.length());
            encrypted.append((char) (c ^ k));
        }
        return encrypted.toString();
    }

    public static String decrypt(String encryptedWord, String key) {
        StringBuilder decrypted = new StringBuilder();
        for (int i = 0; i < encryptedWord.length(); i++) {
            char c = encryptedWord.charAt(i);
            char k = key.charAt(i % key.length());
            decrypted.append((char) (c ^ k));
        }
        return decrypted.toString();
    }

}
