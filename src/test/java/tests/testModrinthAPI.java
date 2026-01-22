package tests;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class testModrinthAPI {

    @Test
    public void testExample() {
        ModrinthAPI api = new ModrinthAPI();
        JSONObject json = api.getJarDetails("lazrs lib", "lastest", "forge");
        System.out.println(json.toString());
    }


}
