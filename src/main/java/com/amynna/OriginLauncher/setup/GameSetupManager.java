package com.amynna.OriginLauncher.setup;

import com.amynna.OriginLauncher.setup.vanillaSetup.McManager;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.FileManager;

/**
 * Classe responsable de la gestion de l'installation du jeu.
 */
public class GameSetupManager {

    /**
     * Gestion de l'installation de Minecraft Vanilla.
     */
    private final McManager mcManager;
    /**
     * Gestion de l'installation de Minecraft Forge.
     */
    private final ForgeManager forgeManager;
    /**
     * Gestion de l'installation du modpack.
     */
    private final ModpackManager modpackManager;

    /**
     * Constructeur de la classe GameSetupManager.
     */
    public GameSetupManager() {
        this.mcManager = new McManager();
        this.forgeManager = new ForgeManager();
        this.modpackManager = new ModpackManager();
    }


    /**
     * Installe le jeu (Minecraft Vanilla).
     */
    public void setupGame() {
        // Crée le répertoire Minecraft s'il n'existe pas
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_DIR.getPath());

        // Installation de Minecraft Vanilla
        mcManager.setupMinecraft();
    }

    /**
     * Désinstalle le jeu.
     */
    public void uninstallGame() {
        FileManager.deleteFileIfExists(AppProperties.MINECRAFT_LIB_DIR);
        FileManager.deleteFileIfExists(AppProperties.MINECRAFT_ASSETS_DIR);
        FileManager.deleteFileIfExists(AppProperties.MINECRAFT_VERSION_DIR);

    }
    /**
     * Répare l'installation du jeu.
     */
    public void repairGame() {
        mcManager.repairMinecraft();
    }
    /**
     * Démarre le jeu.
     */
    public void startGame() {
        mcManager.startMinecraft();
    }



}
