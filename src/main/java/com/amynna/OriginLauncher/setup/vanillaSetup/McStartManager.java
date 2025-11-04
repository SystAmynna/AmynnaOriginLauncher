package com.amynna.OriginLauncher.setup.vanillaSetup;

import com.amynna.OriginLauncher.App;
import com.amynna.OriginLauncher.Config;
import com.amynna.Tools.AppProperties;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Classe responsable de la gestion du démarrage de Minecraft.
 */
public class McStartManager {

    // ----[ ATTRIBUTS ]----

    /** Arguments du jeu */
    private final JSONObject arguments;
    /** Classe principale du jeu */
    private final String mainClass;
    /** Configuration de logging */
    private final JSONObject logging;

    private final String versionType;

    /** Classpath complet pour le lancement */
    private final String classpath;
    /** Nom de l'index des assets */
    private final String assetIndexName;

    /**
     * Constructeur de la classe McStartManager.
     * @param args Les arguments de lancement au format JSON.
     * @param mainClass La classe principale à exécuter.
     * @param logging La configuration de logging au format JSON.
     * @param classpath Le classpath complet pour le lancement.
     * @param assetIndexName Le nom de l'index des assets.
     */
    protected McStartManager(JSONObject args, String mainClass, JSONObject logging, String classpath, String assetIndexName, String versionType) {
        // Initialisation des arguments
        arguments = args;
        assert arguments != null && !arguments.isEmpty();

        // Récupération de la classe principale
        this.mainClass = mainClass;
        assert !this.mainClass.isEmpty();

        // Initialisation de la configuration de logging
        this.logging = logging;
        assert logging != null && !this.logging.isEmpty();

        // Initialisation du classpath
        this.classpath = classpath;
        assert this.classpath != null && !this.classpath.isEmpty();

        // Initialisation du nom de l'index des assets
        this.assetIndexName = assetIndexName;
        assert this.assetIndexName != null && !this.assetIndexName.isEmpty();

        // Initialisation du type de version
        this.versionType = versionType;
        assert this.versionType != null && !this.versionType.isEmpty();
    }

    // ----[ MÉTHODES PRIVÉES ]----

    /**
     * Résout tous les placeholders ${...} présents dans une chaîne.
     * @param input La chaîne contenant potentiellement des placeholders.
     * @return La chaîne avec les placeholders remplacés par leurs valeurs concrètes.
     */
    private String resolvePlaceholders(String input) {
        String output = input;

        // Remplacement des variables JVM spécifiques
        output = output.replace("${natives_directory}", AppProperties.MINECRAFT_NATIVES_DIR.getPath());
        output = output.replace("${classpath}", classpath); // Bien que le classpath soit souvent ajouté à part
        output = output.replace("${launcher_name}", AppProperties.APP_NAME);
        output = output.replace("${launcher_version}", AppProperties.APP_VERSION);

        // Remplacement des variables du jeu spécifiques
        output = output.replace("${auth_player_name}", App.get().getAuth().getMsAuthResult().getProfile().getName());
        output = output.replace("${version_name}", AppProperties.MINECRAFT_VERSION);
        output = output.replace("${game_directory}", AppProperties.MINECRAFT_DIR.getPath());
        output = output.replace("${assets_root}", AppProperties.MINECRAFT_ASSETS_DIR.getPath());
        output = output.replace("${assets_index_name}", assetIndexName);
        output = output.replace("${auth_uuid}", App.get().getAuth().getMsAuthResult().getProfile().getId());
        output = output.replace("${auth_access_token}", App.get().getAuth().getMsAuthResult().getAccessToken());
        output = output.replace("${clientid}", App.get().getAuth().getMsAuthResult().getClientId());
        output = output.replace("${auth_xuid}", App.get().getAuth().getMsAuthResult().getXuid());
        output = output.replace("${user_type}", "msa"); // Toujours "msa" pour Microsoft
        output = output.replace("${version_type}", versionType);
        output = output.replace("${resolution_width}", Config.get().getCustom_width() + "");
        output = output.replace("${resolution_height}", Config.get().getCustom_height() + "");
        output = output.replace("${quickPlayPath}", "..."); // TODO chemin quick play (à voir plus tard)
        output = output.replace("${quickPlaySingleplayer}", "..."); // NE DOIS JAMAIS ÊTRE UTILISÉ
        output = output.replace("${quickPlayMultiplayer}", AppProperties.QUICK_PLAY_MULTIPLAYER_VALUE);
        output = output.replace("${quickPlayRealms}", "..."); // NE DOIS JAMAIS ÊTRE UTILISÉ


        return output;
    }


    /**
     * Construit la liste des arguments JVM en utilisant le manifeste.
     * @return La liste finale des arguments JVM.
     */
    private List<String> buildArguments(JSONArray arguments) {
        // Liste pour stocker les arguments JVM
        List<String> jvmArgs = new LinkedList<>();

        // Parcours de chaque argument dans le tableau
        for (int i = 0; i < arguments.length(); i++) {
            // Récupération de l'argument actuel
            Object arg = arguments.get(i);

            // Analyse et ajout des arguments résolus
            List<String> parsed = parseArg(arg);
            // Si l'argument n'est pas ignoré, on l'ajoute à la liste finale
            if (parsed != null) jvmArgs.addAll(parsed);
        }

        // Retour de la liste finale des arguments JVM
        return jvmArgs;
    }

    /**
     * Analyse un élément du tableau 'arguments.jvm' qui peut être une String simple ou un objet complexe
     * avec des règles et/ou des valeurs.
     *
     * @param arg L'objet JSON à analyser (soit une String, soit un JSONObject).
     * @return Une liste de Strings représentant les arguments ajoutés, ou null si l'argument
     * est ignoré (à cause des règles).
     */
    private List<String> parseArg(Object arg) {
        // Cas 1: L'argument est une simple String (ex: "-Djava.net.preferIPv4Stack=true")
        if (arg instanceof String) return List.of(resolvePlaceholders((String) arg));

        // Cas 2: L'argument est un JSONObject avec des règles ou une seule valeur
        else if (arg instanceof JSONObject argObject) {

            // 2a. Évaluation des règles
            if (argObject.has("rules")) {
                if (!evaluateRules(argObject.getJSONArray("rules"))) {
                    // Les règles ne sont pas satisfaites, on ignore cet argument
                    return null;
                }
            }

            // 2b. Extraction de la valeur
            Object value = argObject.get("value");

            if (value instanceof String) {
                // Valeur simple (ex: "--demo")
                return List.of(resolvePlaceholders((String) value));
            } else if (value instanceof JSONArray) {
                // Valeurs multiples (ex: "--width", "${resolution_width}")
                List<String> resolvedValues = new LinkedList<>();
                JSONArray valuesArray = (JSONArray) value;
                for (int i = 0; i < valuesArray.length(); i++) {
                    // On résout les placeholders pour chaque partie
                    resolvedValues.add(resolvePlaceholders(valuesArray.getString(i)));
                }
                return resolvedValues;
            }
        }
        return null; // Si le format est inconnu
    }

    /**
     * Évalue les règles d'un argument JVM pour déterminer s'il doit être utilisé.
     * Les règles JVM sont basées uniquement sur l'OS.
     * @param rulesArray Le tableau JSON des règles.
     * @return true si l'argument doit être inclus, false sinon.
     */
    private boolean evaluateRules(JSONArray rulesArray) {
        boolean pass = false; // Par défaut, non inclus s'il y a des règles

        for (int i = 0; i < rulesArray.length(); i++) {
            JSONObject rule = rulesArray.getJSONObject(i);
            String action = rule.getString("action");

            // Si aucune condition n'est spécifiée, l'action s'applique toujours.
            boolean conditionMet = true;

            if (rule.has("os")) {
                JSONObject osRule = rule.getJSONObject("os");
                // Vérifie si le nom de l'OS correspond
                conditionMet = osRule.getString("name").equals(AppProperties.getOsType());
                if (osRule.has("arch")) {
                    // Vérifie si l'architecture de l'OS correspond
                    conditionMet = osRule.getString("arch").equals(AppProperties.getOsArch());
                }
                // (Optionnel: Gérer les règles de version OS si elles existent, mais rares en JVM)
            }

            if (rule.has("features")) {
                JSONObject features = rule.getJSONObject("features");
                while (features.keys().hasNext()) {
                    String featureKey = features.keys().next();
                    boolean featureValue = features.getBoolean(featureKey);
                    conditionMet = conditionMet && evaluateFeature(featureKey, featureValue);
                }
            }

            if (conditionMet) {
                if (action.equals("allow")) {
                    pass = true;
                } else if (action.equals("disallow")) {
                    pass = false; // Une règle "disallow" peut annuler un "allow" précédent
                }
            }
        }

        return pass;
    }

    /**
     * Évalue une fonctionnalité spécifique pour les règles JVM.
     * @param featureKey La clé de la fonctionnalité.
     * @param featureValue La valeur attendue de la fonctionnalité.
     * @return true si la fonctionnalité est satisfaite, false sinon.
     */
    private boolean evaluateFeature(String featureKey, boolean featureValue) {
        return switch (featureKey) {
            case "is_demo_user" -> false;
            case "has_custom_resolution" -> featureValue && Config.get().isHas_custom_resolution();
            case "has_quick_plays_support" -> false; // TODO regarder ce que c'est
            case "is_quick_play_singleplayer" -> false; // TODO pas prévue
            case "is_quick_play_multiplayer" -> featureValue && Config.get().isIs_quick_play_multiplayer();
            case "is_quick_play_realms" -> false; // jamais prévue
            default -> false;
        };
    }

    /**
     * Construit la commande de lancement complète (méthode de haut niveau).
     * @return Une liste d'arguments de la ligne de commande Java.
     */
    public List<String> buildLaunchCommand() {
        List<String> command = new LinkedList<>();

        // 1. Début de la commande (exécutable Java)
        command.add(AppProperties.foundJava());

        // Ajout des options de la JVM spécifiques au launcher
        command.add("-Xms" + Config.get().getMinRam() + "G");
        command.add("-Xmx" + Config.get().getMaxRam() + "G");

        // 2. Arguments JVM
        JSONArray jvmArguments = arguments.getJSONArray("jvm");
        command.addAll(buildArguments(jvmArguments));

        // 3. Classe principale
        command.add(mainClass);

        // 4. Arguments du Jeu
        JSONArray gameArguments = arguments.getJSONArray("game");
        command.addAll(buildArguments(gameArguments)); // Pour la prochaine étape

        return command;
    }



    public void startMinecraft() {

        List<String> launchCommand = buildLaunchCommand();

        // Démarrage du processus Minecraft
        ProcessBuilder processBuilder = new ProcessBuilder(launchCommand);
        processBuilder.directory(AppProperties.MINECRAFT_DIR); // Définir le répertoire de travail

        try {
            Process process = processBuilder.start();
            // Optionnel: Gérer les flux d'entrée/sortie du processus si nécessaire
        } catch (Exception e) {
            e.printStackTrace();
            // Gérer les erreurs de démarrage du processus
        }

    }





}
