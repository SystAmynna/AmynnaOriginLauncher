package fr.amynna.OriginLauncher;

import fr.amynna.OriginLauncher.tools.Setup;

public class App {

    private static void run() {

        Setup.process();

        Auth auth = new Auth();
        auth.process();

    }

    public static void main(String[] args) {
        // Lancement de l'application
        run();

    }

}
