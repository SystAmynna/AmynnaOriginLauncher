package com.amynna.Tools;

import javax.swing.*;
import java.awt.*;

/**
 * Classe utilitaire pour afficher des boîtes de dialogue et demander des informations à l'utilisateur.
 */
public class Asker {

    /**
     * Affiche une boîte de dialogue pour demander la création d'un mot de passe avec confirmation.
     *
     * @return le mot de passe haché en SHA-512 si les mots de passe correspondent, null si l'utilisateur annule.
     */
    public static String askFirstPassword() {
        while (true) {
            JPasswordField passwordField = new JPasswordField(20);
            JPasswordField confirmPasswordField = new JPasswordField(20);

            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            panel.add(new JLabel("Mot de passe :"));
            panel.add(passwordField);
            panel.add(new JLabel("Confirmation :"));
            panel.add(confirmPasswordField);

            int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Création du mot de passe",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                return null; // Annulation
            }

            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (password.equals(confirmPassword)) {
                return Encrypter.sha512(password);
            } else {
                JOptionPane.showMessageDialog(
                    null,
                    "Les mots de passe ne correspondent pas.\nVeuillez réessayer.",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Affiche une boîte de dialogue pour demander un mot de passe.
     *
     * @return le mot de passe haché en SHA-512, ou null si l'utilisateur annule.
     */
    public static String askPassword() {
        JPasswordField passwordField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.add(new JLabel("Mot de passe :"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "Saisie du mot de passe",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            return Encrypter.sha512(new String(passwordField.getPassword()));
        } else {
            return null; // Annulation
        }
    }

    /**
     * Affiche une boîte de dialogue de confirmation avec le message fourni.
     * @param message Le message à afficher.
     * @return true si l'utilisateur confirme, false sinon.
     */
    public static boolean confirmAction(String message) {
        int result = JOptionPane.showConfirmDialog(
            null,
            message,
            "Confirmation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Affiche une popup d'information avec le message fourni.
     * @param message Le message à afficher.
     */
    public static void askInfo(String message) {
        JOptionPane.showMessageDialog(
            null,
            message,
            "Information",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Affiche une boîte de dialogue pour demander l'authentification de l'utilisateur.
     *
     * @return un tableau de chaînes contenant l'email et le mot de passe, ou null si l'utilisateur annule.
     */
    public static String[] askAuthentication() {
        String titre = "Authentification Launcher Origin";
        String message = "Veuillez entrer vos identifiants Microsoft pour vous connecter.";
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));

        // Ajout du message
        panel.add(new JLabel(message));

        // Champs de saisie
        JTextField emailField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);

        // Ajout des composants
        JPanel emailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        emailPanel.add(new JLabel("Email:"));
        emailPanel.add(emailField);
        panel.add(emailPanel);

        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordPanel.add(new JLabel("Mot de passe:"));
        passwordPanel.add(passwordField);
        panel.add(passwordPanel);

        // Affichage du dialogue
        int result = JOptionPane.showConfirmDialog(null, panel, titre,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());

            // Vérification des champs
            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                        "L'email et le mot de passe sont obligatoires",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return askAuthentication();
            }

            return new String[]{email, password};
        }
        return null;
    }

    /**
     * Affiche un menu principal et retourne le choix de l'utilisateur.
     *
     * @return un entier représentant le choix de l'utilisateur :
     *         0 pour "Lancer le jeu",
     *         1 pour "Vérifier installation",
     *         2 pour "Se connecter",
     *         3 pour "Paramètres",
     *         4 pour "Admin mode",
     *         -1 si la boîte de dialogue est fermée.
     */
    public static int askMenu() {
        String[] options = {
                "Lancer le jeu",
                "Vérifier installation",
                "Se connecter",
                "Paramètres",
                "Admin mode"
        };

        String msg = "Bienvenue dans le Launcher Origin !\n" +
                     "Veuillez choisir une option pour continuer.";

        return JOptionPane.showOptionDialog(
                null,
                msg,
                "Menu Principal",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
    }



}
