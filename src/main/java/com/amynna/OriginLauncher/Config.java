package com.amynna.OriginLauncher;

public class Config {

    private static Config instance;

    private boolean is_quick_play_multiplayer = false;

    private boolean has_custom_resolution = false;
    private int custom_width = 800;
    private int custom_height = 600;

    private int minRam = 4;
    private int maxRam = 8;


    public static Config get() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public boolean isIs_quick_play_multiplayer() {
        return is_quick_play_multiplayer;
    }

    public boolean isHas_custom_resolution() {
        return has_custom_resolution;
    }

    public  int getCustom_width() {
        return custom_width;
    }
    public  int getCustom_height() {
        return custom_height;
    }
    public  int getMinRam() {
        return minRam;
    }
    public int getMaxRam() {
        return maxRam;
    }



}
