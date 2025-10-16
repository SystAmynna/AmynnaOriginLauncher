package com.amynna.OriginLauncher;

import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Asker;
import com.amynna.Tools.Logger;

public final class App {

    public void launch() {

    }


    public static void main(String[] args) {

        if (args.length == 0) {
            Asker.askInfo("Vous n'êtes pas sensé démarrer ce programme directement.\n" +
                    "Veuillez utiliser le launcher fourni. (" +
                    AppProperties.APP_NAME + "-X.X.X.jar)\n" +
                    "Voir le serveur Discord pour plus d'informations.\n\n" +
                    "Application par " + AppProperties.APP_AUTHOR +
                    " | Version " + AppProperties.APP_VERSION);
            return;
        }

        if (args[0].equals("version")) {
            Logger.version();
        } else if (args[0].equals("launch")) {
            App app = new App();
            app.launch();
        }

    }

}
