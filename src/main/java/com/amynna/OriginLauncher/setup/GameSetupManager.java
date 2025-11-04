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



    public void setupGame() {
        // Crée le répertoire Minecraft s'il n'existe pas
        FileManager.createDirectoriesIfNotExist(AppProperties.MINECRAFT_DIR.getPath());



    }

    public void lightCheckGameSetup() {
    }

    public void checkGameSetup() {
    }

    public void uninstallGame() {
    }





}
