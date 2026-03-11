package tests;

import com.amynna.OriginLauncher.output.Logger;
import org.junit.jupiter.api.Test;

public class TestLogger2 {


    @Test
    void testLog() {
        Logger.log(Logger.LOG_CANAL.MAIN, Logger.VERBOSE_LEVEL.STANDARD, "Test log standard");
        Logger.warn(Logger.LOG_CANAL.MAIN, Logger.VERBOSE_LEVEL.STANDARD, "Test log warning");
        Logger.error(Logger.LOG_CANAL.MAIN, Logger.VERBOSE_LEVEL.STANDARD,"Test log error");
    }


}
