package com.amynna.OriginLauncher;

public class App {

    public static final String VERSION = "0.0.1";


    public static void main(String[] args) {

        if (args.length > 0 && args[0].equals("version")) {
            System.out.println(VERSION);
            System.exit(0);
        }

    }

}
