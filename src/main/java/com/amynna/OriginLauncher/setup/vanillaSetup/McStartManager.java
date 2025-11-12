package com.amynna.OriginLauncher.setup.vanillaSetup;

import com.amynna.OriginLauncher.App;
import com.amynna.OriginLauncher.Config;
import com.amynna.OriginLauncher.setup.global.McCommand;
import com.amynna.OriginLauncher.setup.global.McLibManager;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Logger;
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

    private final String versionType;

    /** Nom de l'index des assets */
    private final String assetIndexName;

    /**
     * Constructeur de la classe McStartManager.
     * @param args Les arguments de lancement au format JSON.
     * @param mainClass La classe principale à exécuter.
     * @param assetIndexName Le nom de l'index des assets.
     */
    public McStartManager(JSONObject args, String mainClass, String assetIndexName, String versionType) {
        // Initialisation des arguments
        arguments = args;
        assert arguments != null && !arguments.isEmpty();

        // Récupération de la classe principale
        this.mainClass = mainClass;
        assert !this.mainClass.isEmpty();

        // Initialisation du nom de l'index des assets
        this.assetIndexName = assetIndexName;
        assert this.assetIndexName != null;

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

        // JVM
        output = output.replace("${natives_directory}", AppProperties.MINECRAFT_NATIVES_DIR.getPath());
        output = output.replace("${classpath}", ""); // NE DOIS JAMAIS ÊTRE UTILISÉ
        output = output.replace("${launcher_name}", AppProperties.APP_NAME);
        output = output.replace("${launcher_version}", AppProperties.APP_VERSION);

        // MC
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
        output = output.replace("${quickPlayPath}", ""); // TODO chemin quick play (à voir plus tard)
        output = output.replace("${quickPlaySingleplayer}", ""); // NE DOIS JAMAIS ÊTRE UTILISÉ
        output = output.replace("${quickPlayMultiplayer}", AppProperties.QUICK_PLAY_MULTIPLAYER_VALUE);
        output = output.replace("${quickPlayRealms}", ""); // NE DOIS JAMAIS ÊTRE UTILISÉ

        // FORGE
        output = output.replace("${library_directory}", AppProperties.MINECRAFT_DIR.getPath());
        output = output.replace("${classpath_separator}", McLibManager.getCpSeparator());

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
        if (arg instanceof String) {
            String argString = (String) arg;
            if (argString.isEmpty() || argString.equals("-cp") || argString.equals("--classpath")) {
                return null; // Ignorer les arguments vides
            }
            String resolved = resolvePlaceholders(argString);
            // Si le placeholder se résout en une chaîne vide, on ignore l'argument.
            return resolved.isEmpty() ? null : List.of(resolved);
        }

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
                String resolved = resolvePlaceholders((String) value);
                // Valeur simple (ex: "--demo")
                return resolved.isEmpty() ? null : List.of(resolved);
            } else if (value instanceof JSONArray) {
                // Valeurs multiples (ex: "--width", "${resolution_width}")
                List<String> resolvedValues = new LinkedList<>();
                JSONArray valuesArray = (JSONArray) value;
                for (int i = 0; i < valuesArray.length(); i++) {
                    String resolvedPart = resolvePlaceholders(valuesArray.getString(i));
                    // On n'ajoute pas les parties qui se résolvent en une chaîne vide.
                    if (!resolvedPart.isEmpty()) {
                        resolvedValues.add(resolvedPart);
                    }
                }
                // Si la liste est vide après résolution, on ignore tout l'argument.
                return resolvedValues.isEmpty() ? null : resolvedValues;
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
                // Vérifie le nom de l'OS
                if (osRule.has("name")) conditionMet = osRule.getString("name").equals(AppProperties.getOsType());
                if (!conditionMet) continue;

                // Vérifie l'architecture de l'OS
                if (osRule.has("arch")) conditionMet = osRule.getString("arch").equals(AppProperties.getOsArch());
                if (!conditionMet) continue;

            }

            if (rule.has("features")) {
                JSONObject features = rule.getJSONObject("features");
                for (String featureKey : features.keySet()) {
                    if (!conditionMet) break;
                    boolean featureValue = features.getBoolean(featureKey);
                    conditionMet = evaluateFeature(featureKey, featureValue);
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
    public void buildLaunchCommand() {

        // 2. Arguments JVM
        JSONArray jvmArguments = arguments.getJSONArray("jvm");
        McCommand.get().addJvmArgs(buildArguments(jvmArguments));

        // 3. Classe principale
        McCommand.get().setMainClass(mainClass);

        // 4. Arguments du Jeu
        JSONArray gameArguments = arguments.getJSONArray("game");
        McCommand.get().addProgramArgs(buildArguments(gameArguments));
    }

}
