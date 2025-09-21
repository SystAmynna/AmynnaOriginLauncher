package com.amynna.Tools;

import java.util.LinkedList;
import java.util.List;

public final class Logger {

    private static final List<String> logMessages = new LinkedList<String>();


    public static void log(String message) {
        System.out.println(message);
        logMessages.add(message);
    }

    public static void error(String message) {
        System.err.println(message);
        logMessages.add("[ERROR]: " + message);
    }

    public static void fatal(String message) {
        error(message);
        saveLogToFile();
        System.exit(1);
    }

    private static void saveLogToFile() {


    }


}
