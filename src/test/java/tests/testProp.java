package tests;

import com.amynna.OriginLauncher.Auth;
import com.amynna.Tools.Asker;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import org.junit.jupiter.api.Test;

public class testProp {


    @Test
    public void testOs() {
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("Operating System: " + os);
    }

    @Test
    public void testAuth() {
        Auth auth = new Auth();
        auth.authentifie();

        System.out.println(auth.getXUID());
    }

    @Test
    public void testLogin() {

        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        MicrosoftAuthResult msAuthResult = null;

        String [] credentials = Asker.askAuthentication();
        assert credentials != null;
        assert credentials.length == 2;

        String email = credentials[0];
        String password = credentials[1];

        try {
            msAuthResult = authenticator.loginWithCredentials(email, password);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assert msAuthResult != null;

    }


}
