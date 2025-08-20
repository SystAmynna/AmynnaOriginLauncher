package fr.amynna.OriginLauncher.work;

import fr.amynna.OriginLauncher.data.Config;
import fr.amynna.OriginLauncher.data.Proprieties;
import fr.amynna.OriginLauncher.install.SetupMc;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Classe pour démarrer Minecraft avec les paramètres appropriés.
 * Elle génère la commande nécessaire et lance le processus.
 */
public class StarterMc {

    /**
     * Liste des arguments de la commande pour démarrer Minecraft.
     */
    List<String> command = new LinkedList<>();

    /**
     * Manifest de version Minecraft.
     * Contient les informations nécessaires pour démarrer Minecraft.
     */
    JSONObject versionManifest;
    /**
     * Index des ressources Minecraft.
     * Utilisé pour récupérer les ressources nécessaires au démarrage.
     */
    JSONObject assetIndex;
    /**
     * Résultat de l'authentification Microsoft.
     * Contient les informations d'authentification nécessaires pour démarrer Minecraft.
     */
    MicrosoftAuthResult authResult;

    /**
     * Constructeur de la classe StarterMc.
     * Charge le manifest de version et l'index des ressources.
     *
     * @param authResult Le résultat de l'authentification Microsoft.
     */
    public StarterMc(MicrosoftAuthResult authResult) {
        this.authResult = authResult;

        versionManifest = FileManager.openJsonFile(SetupMc.VERSION_MANIFEST_PATH);

        if (versionManifest == null) {
            Printer.fatalError("Le manifest de version n'a pas pu être chargé. Assurez-vous que Minecraft est installé.");
            return;
        }
        assetIndex = versionManifest.getJSONObject("assetIndex");

    }

    /**
     * Génère la commande pour démarrer Minecraft avec les paramètres appropriés.
     * Cette méthode construit la commande en fonction des propriétés et de l'authentification.
     */
    public void genCmd() {
        List<String> classpath = new ArrayList<>();


        classpath.add(Paths.get(Proprieties.MC_PATH, "versions", Proprieties.MINECRAFT_VERSION,
                Proprieties.MINECRAFT_VERSION + ".jar").toString());
        try {
            Files.walk(Paths.get(Proprieties.MC_PATH, "libraries"))
                    .filter(p -> p.toString().endsWith(".jar"))
                    .forEach(p -> classpath.add(p.toString()));
        } catch (IOException e) {
            Printer.fatalError("Erreur lors de la récupération des bibliothèques Minecraft : " + e.getMessage());
            return;
        }

        String cp = String.join(File.pathSeparator, classpath);

        // Récupération des informations d'authentification
        String playerName = authResult.getProfile().getName();
        String playerUUID = authResult.getProfile().getId();
        String accessToken = authResult.getAccessToken();

        command.clear();

        // === Commande de base ===
        command.add(Proprieties.foundJava());

        // === Options JVM ===
        int minMemory = Config.getIntConfig("minRAM");
        int maxMemory = Config.getIntConfig("maxRAM");
        minMemory = minMemory == 0 ? 4 : minMemory; // Valeur par défaut si minRAM est 0
        maxMemory = maxMemory == 0 ? 6 : maxMemory; // Valeur par défaut si maxRAM est 0

        command.add("-Xms" + minMemory + "G"); // Mémoire minimale
        command.add("-Xmx" + maxMemory + "G"); // Mémoire maximale

        // natives
        command.add("-Djava.library.path=" + Paths.get(Proprieties.MC_PATH, "natives").toString());

        // === Classpath ===
        command.add("-cp");
        command.add(cp);

        // === Main class ===
        command.add(versionManifest.getString("mainClass"));

        // === Arguments du jeu ===
        command.add("--username");
        command.add(playerName);

        command.add("--uuid");
        command.add(playerUUID);

        command.add("--accessToken");
        command.add(accessToken);

        command.add("--version");
        command.add(Proprieties.MINECRAFT_VERSION);

        command.add("--gameDir");
        command.add(Proprieties.MC_PATH);

        command.add("--assetsDir");
        command.add(Paths.get(Proprieties.MC_PATH, "assets").toString());

        command.add("--assetIndex");
        command.add(assetIndex.getString("id"));

        // arguments supplémentaires requis par Mojang
        command.add("--userType");
        command.add("msa"); // Microsoft account auth

        command.add("--versionType");
        command.add("release");
    }

    /**
     * Démarre Minecraft avec la commande générée.
     * Assure que la commande a été générée avant de démarrer le processus.
     */
    public void start() {
        if (command.isEmpty()) {
            Printer.fatalError("La commande n'a pas été générée. Veuillez appeler genCmd() avant de démarrer Minecraft.");
            return;
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.inheritIO(); // Pour afficher la sortie dans la console
            Process process = processBuilder.start();
            process.waitFor(); // Attendre que le processus se termine
        } catch (IOException | InterruptedException e) {
            Printer.fatalError("Erreur lors du démarrage de Minecraft : " + e.getMessage());
        }
    }




}
