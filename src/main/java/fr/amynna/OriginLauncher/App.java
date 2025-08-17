package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.work.Auth;
import fr.amynna.OriginLauncher.work.Setup;
import fr.amynna.OriginLauncher.work.SetupMc;

public class App {

    private static void run() {

        Setup.process();

        Auth auth = new Auth();
        auth.process();

        SetupMc.process();


    }

    public static void main(String[] args) {
        // Lancement de l'application
        run();

    }

}
