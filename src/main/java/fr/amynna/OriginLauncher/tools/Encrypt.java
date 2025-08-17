package fr.amynna.OriginLauncher.tools;

/**
 * Classe {@code Encrypt} fournit des méthodes pour chiffrer et déchiffrer des chaînes de caractères
 * en utilisant une clé de chiffrement simple.
 */
public class Encrypt {

    /**
     * Chiffre un mot en utilisant une clé de chiffrement.
     *
     * @param word le mot à chiffrer
     * @param key  la clé de chiffrement
     * @return le mot chiffré
     */
    public static String encrypt(String word, String key) {
        StringBuilder encrypted = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            char k = key.charAt(i % key.length());
            encrypted.append((char) (c ^ k));
        }
        return encrypted.toString();
    }

    /**
     * Déchiffre un mot en utilisant une clé de chiffrement.
     *
     * @param encryptedWord le mot chiffré à déchiffrer
     * @param key           la clé de chiffrement
     * @return le mot déchiffré
     */
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
