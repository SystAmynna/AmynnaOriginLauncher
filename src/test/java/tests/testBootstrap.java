package tests;

import com.amynna.Tools.Asker;
import com.amynna.Tools.KeyUtil;
import com.amynna.Tools.Logger;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class testBootstrap {


   @Test
    public void testAskFirstPassword() {

       String password = Asker.askFirstPassword();

       assert password != null;

       Logger.log("password: " + password);


   }

    @Test
    public void testKeys1() {




   }







}
