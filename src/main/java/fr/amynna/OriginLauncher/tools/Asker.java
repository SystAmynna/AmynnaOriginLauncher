package fr.amynna.OriginLauncher.tools;

import javax.swing.*;
import java.awt.*;

public class Asker {

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



}
