package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.data.Config;
import fr.amynna.OriginLauncher.install.SetupModLoader;
import fr.amynna.OriginLauncher.tools.Printer;
import fr.amynna.OriginLauncher.work.Auth;
import fr.amynna.OriginLauncher.install.Setup;
import fr.amynna.OriginLauncher.install.SetupMc;
import fr.amynna.OriginLauncher.work.StarterMc;

public class App {

    private static Auth auth = null;

    private static void run() {

        Setup.process();

        Config.loadConfig();

        auth = new Auth();
        auth.process();

        SetupMc.process();
        SetupModLoader.process();

        // TODO UI

        // TODO : temporary call
        startMinecraft();

    }

    public static void startMinecraft() {
        // Lancement de Minecraft
        StarterMc starterMc = new StarterMc(auth.getMsAuthResult());
        starterMc.genCmd();
        starterMc.start();
    }

    public static void main(String[] args) {
        // Lancement de l'application
        run();

    }

}
