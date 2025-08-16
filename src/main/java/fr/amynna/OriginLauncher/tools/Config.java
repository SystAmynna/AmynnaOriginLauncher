package fr.amynna.OriginLauncher.tools;

import java.nio.file.Path;

public class Config {

    public static final Config INSTANCE = new Config();

    private static final Path WORKING_DIR = Path.of(System.getProperty("user.home"), ".OriginRP");

    private static final Path MC_DIR = WORKING_DIR.resolve(".minecraft");
    private static final Path LAUNCHER_DIR = WORKING_DIR.resolve("launcher");
    private static final Path TEMP_DIR = LAUNCHER_DIR.resolve("temp");


    private Config() {
        System.out.println("Config instance created");
    }

    public  static Path getWorkingDir() {
        return WORKING_DIR;
    }
    public static Path getMinecraftDir() {
        return MC_DIR;
    }
    public static Path getLauncherDir() {
        return LAUNCHER_DIR;
    }
    public static Path getTempDir() {
        return TEMP_DIR;
    }

}
