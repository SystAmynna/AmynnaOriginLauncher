package com.amynna.Tools;

import com.amynna.OriginLauncher.setup.modpack.MpFilesManager;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.awt.Desktop;

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

    public static void askOptionnalMods(List<MpFilesManager.OptionalMpFile> list) {
        if (list == null || list.isEmpty()) return;

        final java.util.LinkedHashMap<MpFilesManager.OptionalMpFile, javax.swing.JCheckBox> map = new java.util.LinkedHashMap<>();

        Runnable showDialog = () -> {
            javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null, "Fichiers optionnels", true);
            javax.swing.JPanel content = new javax.swing.JPanel();
            content.setLayout(new javax.swing.BoxLayout(content, javax.swing.BoxLayout.Y_AXIS));
            content.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

            for (MpFilesManager.OptionalMpFile mpFile : list) {
                String display = mpFile.getName() + " : " + mpFile.getDescription();
                javax.swing.JCheckBox cb = new javax.swing.JCheckBox(display, mpFile.isEnabled());
                map.put(mpFile, cb);
                content.add(cb);
            }

            javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(content);
            scroll.setPreferredSize(new java.awt.Dimension(500, Math.min(400, 30 * map.size()) + 30));

            // Ajouter un léger espace


            javax.swing.JPanel buttons = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
            javax.swing.JButton ok = new javax.swing.JButton("OK");
            javax.swing.JButton cancel = new javax.swing.JButton("Annuler");
            buttons.add(cancel);
            buttons.add(ok);

            javax.swing.JPanel main = new javax.swing.JPanel(new java.awt.BorderLayout());
            main.add(scroll, java.awt.BorderLayout.CENTER);
            main.add(buttons, java.awt.BorderLayout.SOUTH);

            dialog.getContentPane().add(main);
            dialog.pack();
            dialog.setLocationRelativeTo(null);

            ok.addActionListener(e -> {
                // Récupère l'état souhaité puis ferme la boîte
                java.util.Map<MpFilesManager.OptionalMpFile, Boolean> desired = new java.util.HashMap<>();
                for (java.util.Map.Entry<MpFilesManager.OptionalMpFile, javax.swing.JCheckBox> entry : map.entrySet()) {
                    desired.put(entry.getKey(), entry.getValue().isSelected());
                }
                dialog.dispose();

                Thread updater = new Thread(() -> {
                    for (java.util.Map.Entry<MpFilesManager.OptionalMpFile, Boolean> entry : desired.entrySet()) {
                        MpFilesManager.OptionalMpFile mpFile = entry.getKey();
                        boolean wantEnabled = entry.getValue();
                        try {
                            if (wantEnabled && !mpFile.isEnabled()) mpFile.enable();
                            else if (!wantEnabled && mpFile.isEnabled()) mpFile.disable();
                        } catch (Exception ex) {
                            Logger.error("Erreur lors de la mise à jour du mod " + mpFile.getName() + " : " + ex.getMessage());
                        }
                    }
                }, "ModsUpdater");
                updater.setDaemon(true);

                // Si on est sur l'EDT, on lance sans bloquer; sinon on attend la fin pour rester synchrone.
                if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                    updater.start();
                } else {
                    updater.start();
                    try { updater.join(); } catch (InterruptedException ignored) {}
                }
            });

            cancel.addActionListener(e -> dialog.dispose());
            dialog.setVisible(true);
        };

        try {
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                showDialog.run();
            } else {
                javax.swing.SwingUtilities.invokeAndWait(showDialog);
            }
        } catch (Exception ex) {
            Logger.error("Impossible d'afficher la fenêtre des mods optionnels : " + ex.getMessage());
        }
    }

    public static void openUrlInBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                Logger.error("Impossible d'ouvrir le navigateur : " + e.getMessage());
            }
        } else {
            // Alternative pour les systèmes où Desktop n'est pas supporté (certains Linux/serveurs)
            Runtime runtime = Runtime.getRuntime();
            try {
                String os = AppProperties.getOsType();
                if (os.contains("win")) {
                    runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("osx")) {
                    runtime.exec("open " + url);
                } else {
                    runtime.exec("xdg-open " + url);
                }
            } catch (IOException e) {
                Logger.error("Échec de l'ouverture manuelle de l'URL : " + e.getMessage());
            }
        }
        Logger.log("Ouvrir : " + url);
    }


}
