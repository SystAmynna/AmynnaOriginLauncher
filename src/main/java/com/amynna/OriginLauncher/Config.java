package com.amynna.OriginLauncher;

import org.json.JSONObject;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


// TODO : Implémenter cette classe pour gérer la configuration du launcher

/**
 * La classe {@code Config} gère la configuration du launcher.
 */
public class Config {

    /** Instance unique de la configuration (singleton). */
    private static Config instance;

    /** Fichier de configuration. */
    private File configFile;

    /** Objet JSON pour stocker les paramètres de configuration. */
    private JSONObject config;

    /** Indicateur pour le mode "Quick Play Multiplayer". */
    private boolean is_quick_play_multiplayer = false;

    /** Indicateur pour une résolution personnalisée. */
    private boolean has_custom_resolution = false;
    /** Largeur personnalisée de la fenêtre du jeu. */
    private int custom_width = 800;
    /** Hauteur personnalisée de la fenêtre du jeu. */
    private int custom_height = 600;

    // TODO : passer en MO plutôt qu'en GO
    /** Mémoire RAM minimale allouée au jeu (en Go). */
    private int minRam = 4;
    /** Mémoire RAM maximale allouée au jeu (en Go). */
    private int maxRam = 8;


    /** Méhode pour charger la configuration depuis un fichier. */
    protected void load() {
        // TODO : Charger la configuration depuis un fichier
    }

    /** Méthode pour sauvegarder la configuration dans un fichier. */
    private void save() {
        // TODO : Sauvegarder la configuration dans un fichier
    }



    // −−−-[ GETTERS ]----

    /** Getteur pour l'instance unique de la configuration.
     * @return {@code Config} L'instance de la configuration.
     */
    public static Config get() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    /** Getteur pour l'indicateur du mode "Quick Play Multiplayer".
     * @return {@code boolean} Vrai si le mode est activé, faux sinon.
     */
    public boolean isIs_quick_play_multiplayer() {
        return is_quick_play_multiplayer;
    }

    /** Getteur pour l'indicateur de résolution personnalisée.
     * @return {@code boolean} Vrai si une résolution personnalisée est définie, faux sinon.
     */
    public boolean isHas_custom_resolution() {
        return has_custom_resolution;
    }
    /** Getteur pour la largeur personnalisée de la fenêtre du jeu.
     * @return {@code int} La largeur personnalisée en pixels.
     */
    public  int getCustom_width() {
        return custom_width;
    }
    /** Getteur pour la hauteur personnalisée de la fenêtre du jeu.
     * @return {@code int} La hauteur personnalisée en pixels.
     */
    public  int getCustom_height() {
        return custom_height;
    }

    /** Getteur pour la mémoire RAM minimale allouée au jeu.
     * @return {@code int} La mémoire minimale en Go.
     */
    public  int getMinRam() {
        return minRam;
    }
    /** Getteur pour la mémoire RAM maximale allouée au jeu.
     * @return {@code int} La mémoire maximale en Go.
     */
    public int getMaxRam() {
        return maxRam;
    }



}
