package com.amynna.OriginLauncher.setup.global;

import com.amynna.OriginLauncher.Config;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Logger;

import java.util.LinkedList;
import java.util.List;

public class McCommand {

    public static final McCommand INSTANCE = new McCommand();

    private String javaExecutable;

    private List<String> jvmArgs;

    private String classpath;

    private String mainClass;

    private List<String> programArgs;

    private McCommand() {
        javaExecutable = AppProperties.foundJava();

        jvmArgs = new LinkedList<>();
        programArgs = new LinkedList<>();

        jvmArgs.addAll(Config.get().getJvmArgs());
    }


    public static McCommand get() {
        return INSTANCE;
    }




    /**
     * Démarre le processus Minecraft avec la commande construite.
     */
    public void startMinecraft() {

        List<String> launchCommand = new LinkedList<String>();

        launchCommand.add(javaExecutable);
        launchCommand.addAll(jvmArgs);

        launchCommand.add("--add-opens");
        launchCommand.add("java.base/java.lang=ALL-UNNAMED");
        launchCommand.add("--add-opens");
        launchCommand.add("java.base/java.util.zip=ALL-UNNAMED");
        launchCommand.add("--add-opens");
        launchCommand.add("java.base/java.security=ALL-UNNAMED");

        String clientPath = AppProperties.MINECRAFT_CLIENT.getAbsolutePath();
        addToClasspath(clientPath);

        launchCommand.add("-cp");
        launchCommand.add(classpath);

        launchCommand.add(mainClass);
        launchCommand.addAll(programArgs);

        logCommand(launchCommand);

        // Démarrage du processus Minecraft
        ProcessBuilder processBuilder = new ProcessBuilder(launchCommand);
        processBuilder.directory(AppProperties.MINECRAFT_DIR); // Définir le répertoire de travail
        processBuilder.inheritIO();

        try {
            Logger.log(Logger.PURPLE + Logger.BOLD + "Starting Minecraft Process");
            Process process = processBuilder.start();
            // Optionnel: Gérer les flux d'entrée/sortie du processus si nécessaire
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }

    }

    public void logCommand(List<String> command) {
        for (String s : command) {
            Logger.log(Logger.BLUE + s + " ");
        }
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void addJvmArg(String arg) {
        this.jvmArgs.add(arg);
    }
    public void addJvmArgs(List<String> args) {
        this.jvmArgs.addAll(args);
    }

    public void addProgramArgs(String arg) {
        this.programArgs.add(arg);
    }
    public void addProgramArgs(List<String> args) {
        this.programArgs.addAll(args);
    }

    public void addToClasspath(String cp) {
        if (this.classpath == null) {
            this.classpath = cp;
        } else {
            this.classpath += McLibManager.getCpSeparator() + cp;
        }
    }


}
