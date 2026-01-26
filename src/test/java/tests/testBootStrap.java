package tests;


import com.amynna.Tools.AppProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.amynna.OriginBootstrap.App;

import java.io.File;

public class testBootStrap {

    @BeforeAll
    static void beforeAll() {

        boolean ready = false;

        File KeyStoreFile = AppProperties.LOCAL_PRIVATE_KEYS_LOCATION;
        File KeyStoreBackupFile = new File( KeyStoreFile.getName() + ".backup");

        if (KeyStoreFile.exists()) {
            ready = KeyStoreFile.renameTo(KeyStoreBackupFile);
        } else {
            ready = true;
        }

        if (!ready) {
            throw new RuntimeException("Impossible de renommer le fichier de keystore avant les tests.");
        }


    }

    @Test
    void testHelp() {

        String [] args = {"help"};

        App.main(args);

    }

    @Test
    void testVersion() {

        String [] args = {"version"};

        App.main(args);

    }

    void testGenKeys() {

        String [] args = {"genKeys", "testAlias"};

        App.main(args);



    }






}
