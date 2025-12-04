package tests;

import com.amynna.OriginLauncher.setup.modpack.ModrinthAPI;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class testModrinthAPI {

    @Test
    public void testExample() {
        ModrinthAPI api = new ModrinthAPI();
        JSONObject json = api.getJarDetails("Chat Plus", "2.7.0", "forge");
        System.out.println(json.toString());
    }


}
