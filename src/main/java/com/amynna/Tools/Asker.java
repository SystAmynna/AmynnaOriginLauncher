package com.amynna.Tools;

import javax.swing.*;
import java.awt.*;

public class Asker {

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



}
