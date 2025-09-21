import com.amynna.OriginBootstrap.App;
import com.amynna.Tools.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;

public class testBootstrap {


    /**
     * Test de la génération des clés publique/privée.
     */
    @Test
    public void testGenKeys() {
        String[] args = {"genKeys"};
        App.main(args);

        File privateKey = new File("private.key");
        File publicKey = new File("public.key");

        assert privateKey.exists();
        assert publicKey.exists();

        // Clean up
        privateKey.delete();
        publicKey.delete();
    }

    /**
     * Test de la signature et de la vérification d'un fichier.
     */
    @Test
    public void testSignAndVerify() {
        // First, generate keys
        String[] genArgs = {"genKeys"};
        App.main(genArgs);

        File privateKey = new File("private.key");
        File publicKey = new File("public.key");

        assert privateKey.exists();
        assert publicKey.exists();

        // Create a sample file to sign
        File sampleFile = new File("sample.txt");
        try {
            if (!sampleFile.exists()) {
                sampleFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        // Sign the sample file
        String[] signArgs = {"sign", "sample.txt", "private.key"};
        App.main(signArgs);

        File signatureFile = new File("sample.txt.sig");
        assert signatureFile.exists();

        // Verify the signature
        String[] verifyArgs = {"verify", "sample.txt", "sample.txt.sig", "public.key"};
        App.main(verifyArgs);

        String logs = Logger.getLogMessages();
        assert logs.contains("✅ Signature is valid.");

        // Clean up
        privateKey.delete();
        publicKey.delete();
        sampleFile.delete();
        signatureFile.delete();
    }





    @Test
    public void testVerifyTrustedSigned() {


    }

}
