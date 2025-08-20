import fr.amynna.OriginLauncher.data.Config;
import fr.amynna.OriginLauncher.tools.Printer;
import org.junit.jupiter.api.Test;

public class ConfigTest {

    @Test
    void testA() {
        Config.loadConfig();

        Printer.debug(Config.getIntConfig("minRAM") + "G " + Config.getIntConfig("maxRAM") + "G");

    }




}
