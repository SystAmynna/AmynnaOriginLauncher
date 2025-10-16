package com.amynna.Tools;

import java.util.LinkedList;
import java.util.List;

public final class Logger {

    private static final List<String> logMessages = new LinkedList<String>();

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";



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

    public static void version() {
        log(AppProperties.APP_NAME + " version " + AppProperties.APP_VERSION + " \nby " + AppProperties.APP_AUTHOR);
    }

    private static void saveLogToFile() {


    }

    public static String getLogMessages() {
        StringBuilder sb = new StringBuilder();
        for (String msg : logMessages) {
            sb.append(msg).append(System.lineSeparator());
        }
        return sb.toString();
    }


}
