package fr.amynna.OriginLauncher.install;

import fr.amynna.OriginLauncher.data.Proprieties;
import fr.amynna.OriginLauncher.tools.FileManager;
import fr.amynna.OriginLauncher.tools.Printer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

public class SetupModLoader {

    private static final String FORGE_VERSION = "47.4.0";

    private static final String MOD_LOADER_VERSION = Proprieties.MINECRAFT_VERSION + "-" + FORGE_VERSION;

    private static final String MOD_LOADER_FILE_NAME = "forge-" + MOD_LOADER_VERSION + "-installer.jar";

    private static final String MOD_LOADER_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/" + MOD_LOADER_VERSION + "/" + MOD_LOADER_FILE_NAME;

    private static final String MOD_LOADER_INSTALLER_PATH = Proprieties.ROOT_PATH + "/" + MOD_LOADER_FILE_NAME ;

    public static void process() {
        Printer.info("Téléchargement du mod loader Forge...");

        // télécharge l'installateur
        File modLoader = FileManager.downloadFile(MOD_LOADER_URL, MOD_LOADER_INSTALLER_PATH);
        if (modLoader == null || !modLoader.exists()) {
            Printer.fatalError("Mod Loader introuvable : " + MOD_LOADER_INSTALLER_PATH);
        }

        List<String> cmd = new LinkedList<>();
        cmd.add(Proprieties.foundJava()); // doit être Java 17+
        cmd.add("-jar");
        cmd.add(modLoader.getAbsolutePath());
        cmd.add("--installClient");
        cmd.add(Proprieties.MC_PATH);

        Printer.info("Installation du mod loader Forge...");

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(Proprieties.MC_PATH)); // exécuter depuis le dossier MC
            pb.inheritIO(); // rediriger stdout/stderr
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                Printer.fatalError("L’installateur Forge s’est terminé avec le code " + exitCode);
            }
        } catch (Exception e) {
            Printer.fatalError("Erreur lors de l'exécution de l’installateur Forge : " + e.getMessage());
        }

        // Vérifier que le manifest Forge a bien été créé
        String forgeVersion = Proprieties.MINECRAFT_VERSION + "-" + FORGE_VERSION;
        Path forgeJson = Paths.get(Proprieties.MC_PATH, "versions", forgeVersion, forgeVersion + ".json");

        if (!Files.exists(forgeJson)) {
            Printer.fatalError("Forge n’a pas généré de version.json. Installation incomplète !");
        }

        Printer.info("Mod loader Forge installé avec succès (" + forgeVersion + ").");
    }




}
