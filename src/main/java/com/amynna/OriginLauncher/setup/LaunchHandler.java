package com.amynna.OriginLauncher.setup;

import com.amynna.OriginLauncher.App;
import com.amynna.OriginLauncher.Config;
import com.amynna.Tools.AppProperties;
import com.amynna.Tools.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/** Gestionnaire du lancement du jeu Minecraft. */
public class LaunchHandler {

    /** Liste des arguments JVM. */
    private final List<String> jvmArgs = new LinkedList<>();
    /** Liste des arguments de jeu. */
    private final List<String> gameArgs = new LinkedList<>();

    /** Nom de l'index des assets. */
    private String assetIndexName;
    /** Type de version du jeu. */
    private String versionType;
    /** Classe principale à exécuter. */
    private String mainClass;

    /** Classpath complet pour le lancement. */
    private String classpath;

    // ----[ SETTERS ]----

    /** Définit le nom de l'index des assets. */
    protected void setAssetIndexName(String assetIndexName) {
        this.assetIndexName = assetIndexName;
    }
    /** Définit le type de version du jeu. */
    protected void setVersionType(String versionType) {
        this.versionType = versionType;
    }
    /** Définit la classe principale à exécuter. */
    protected void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
    /** Définit le classpath complet pour le lancement. */
    protected void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    /**
     * Charge les arguments à partir d'un JSON de manifeste.
     */
    protected void loadManifest(JSONObject args) {
        assert args != null;
        assert !args.isEmpty();

        // Récupération des sections "jvm" et "game"
        final JSONArray jvm = args.optJSONArray("jvm");
        assert  jvm != null;
        final JSONArray game = args.optJSONArray("game");
        assert game != null;

        // Ajout des arguments JVM
        jvmArgs.addAll(extractArguments(jvm));
        // Ajout des arguments de jeu
        gameArgs.addAll(extractArguments(game));
    }


    /**
     * Extrait les arguments JVM à partir du tableau JSON 'arguments.jvm'.
     *
     * @param argsArray Le tableau JSON des arguments JVM.
     * @return Une liste de Strings représentant les arguments JVM extraits.
     */
    private List<String> extractArguments(JSONArray argsArray) {
        // Liste pour stocker les arguments extraits
        List<String> extractedArgs = new LinkedList<>();

        // Parcours de chaque élément du tableau 'arguments.jvm'
        for (int i = 0; i < argsArray.length(); i++) {
            // Récupération de l'élément courant
            Object arg = argsArray.get(i);
            // Analyse de l'argument
            List<String> parsedArgs = parseArg(arg);
            // Si des arguments ont été extraits, on les ajoute à la liste finale
            if (parsedArgs != null) extractedArgs.addAll(parsedArgs);
        }

        // Retourne la liste des arguments extraits
        return extractedArgs;
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

            String resolved = resolvePlaceholders(arg.toString());
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
            } else if (value instanceof JSONArray valuesArray) {
                // Valeurs multiples (ex: "--width", "${resolution_width}")
                List<String> resolvedValues = new LinkedList<>();
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
     * Résout tous les placeholders ${...} présents dans une chaîne.
     * @param input La chaîne contenant potentiellement des placeholders.
     * @return La chaîne avec les placeholders remplacés par leurs valeurs concrètes.
     */
    private String resolvePlaceholders(String input) {
        String output = input;

        // JVM
        output = output.replace("${natives_directory}", AppProperties.MINECRAFT_NATIVES_DIR.getPath());
        output = output.replace("${classpath}", classpath); // NE DOIS JAMAIS ÊTRE UTILISÉ
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
        output = output.replace("${library_directory}", "libraries");
        output = output.replace("${classpath_separator}", AppProperties.getCpSeparator());

        return output;
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
     * Construit la commande finale de lancement
     */
    private List<String> buildLaunchCommand() {
        List<String> cmd = new LinkedList<>();

        // Java exécutable
        cmd.add(AppProperties.foundJava());



        // Ajout des options de mémoire
        cmd.add("-Xmx" + Config.get().getMaxRam() + "G");
        cmd.add("-Xms" + Config.get().getMinRam() + "G");
        // Arguments JVM
        cmd.addAll(jvmArgs);


        // Main class
        cmd.add(mainClass);

        // Arguments de jeu
        cmd.addAll(gameArgs);

        return cmd;
    }

    /**
     * Lance le jeu
     */
    public void start() {
        List<String> command = buildLaunchCommand();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(AppProperties.MINECRAFT_DIR);
        pb.inheritIO(); // redirige la sortie du jeu vers la console

        Logger.log("Démarrage de Minecraft avec Forge...");
        try {
            pb.start();
        } catch (IOException e) {
            Logger.error("Erreur lors du démarrage de Minecraft : " + e.getMessage());
        }

    }

}
