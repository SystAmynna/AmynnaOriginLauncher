package com.amynna.OriginLauncher;

public abstract class App {

    private static App instance;

    protected void init() {

    }

    protected abstract void run();

    protected abstract void appEnd();

    protected static void end() {

        get().appEnd();

        Logger.close();
        System.exit(0);
    }

    public static App get() {
        if (instance == null) end();
        return instance;
    }




}
