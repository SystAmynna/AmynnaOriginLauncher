package tests;

import com.amynna.OriginBootstrap.App;
import com.amynna.Tools.Asker;
import com.amynna.Tools.Logger;
import org.junit.jupiter.api.Test;

import java.io.File;

public class testBootstrap {


   @Test
    public void testAskFirstPassword() {

       String password = Asker.askFirstPassword();

       assert password != null;

       Logger.log("password: " + password);


   }







}
