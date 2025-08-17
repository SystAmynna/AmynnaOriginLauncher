package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.data.Config;
import fr.amynna.OriginLauncher.tools.Printer;
import fr.amynna.OriginLauncher.work.Auth;
import fr.amynna.OriginLauncher.work.Setup;
import fr.amynna.OriginLauncher.work.SetupMc;
import fr.amynna.OriginLauncher.work.StarterMc;

public class App {

    private static void run() {

        Setup.process();

        Config.loadConfig();

        Auth auth = new Auth();
        auth.process();

        SetupMc.process();

        // TODO UI


        Printer.printDebug("RAM " + Config.getIntConfig("minRAM") + "G " + Config.getIntConfig("maxRAM") + "G");

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
