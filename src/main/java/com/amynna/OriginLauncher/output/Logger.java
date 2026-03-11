package com.amynna.OriginLauncher.output;

import com.amynna.OriginLauncher.App;
import com.amynna.OriginLauncher.AppProperties;
import com.amynna.OriginLauncher.FileManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/** Gère la sortie du programme et les logs dans un fichier */
public final class Logger {

    /** Niveau de Verbose*/
    public enum VERBOSE_LEVEL {
        SILENT,
        STANDARD,
        VERBOSE,
        VERY_VERBOSE
    }
    /** Canal de logs */
    public enum LOG_CANAL {
        MAIN,
        GRAPHIC,
        FILES,
        KEYS,
        DOWNLOAD
    }
    /** Niveau de Verbose par canal */
    private static final Map<LOG_CANAL, VERBOSE_LEVEL> CANAL_VERBOSE_LEVEL = new EnumMap<>(LOG_CANAL.class);
    static {
        CANAL_VERBOSE_LEVEL.put(LOG_CANAL.MAIN, VERBOSE_LEVEL.VERY_VERBOSE);
        CANAL_VERBOSE_LEVEL.put(LOG_CANAL.GRAPHIC, VERBOSE_LEVEL.VERY_VERBOSE);
        CANAL_VERBOSE_LEVEL.put(LOG_CANAL.FILES, VERBOSE_LEVEL.VERY_VERBOSE);
        CANAL_VERBOSE_LEVEL.put(LOG_CANAL.KEYS, VERBOSE_LEVEL.VERY_VERBOSE);
        CANAL_VERBOSE_LEVEL.put(LOG_CANAL.DOWNLOAD, VERBOSE_LEVEL.VERY_VERBOSE);
    }

    public static final String RESET  = "\u001B[0m";

    public static final String BOLD      = "\u001B[1m";
    public static final String ITALIC    = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String EOL       = System.lineSeparator();

    public static final String RED    = "\u001B[31m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE   = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN   = "\u001B[36m";
    public static final String WHITE  = "\u001B[37m";

    private static final String ANSI_REGEX = "\u001B\\[[;\\d]*m";

    /** Formateur de temps */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Writer vers le fichier de log */
    private static PrintWriter fileWriter;

    /** Initialise le fichier de log */
    public static void init() {
        FileManager.createDirectoriesIfNotExist(AppProperties.LOG_DIR);
        final String fileName = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".log";
        final Path logFile = AppProperties.LOG_DIR.toPath().resolve(fileName);
        try {
            fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFile.toFile(), true)), true);
        } catch (IOException e) {
            error(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, e, "Impossible d'initialiser le fichier de log,", Logger.BOLD, " les logs ne seront pas sauvegardée !");
        }
    }

    /** Écrit un message dans le fichier de log (sans les codes ANSI) */
    private static void writeToFile(String msg) {
        if (fileWriter != null) {
            fileWriter.println(clearAnsiRegex(msg));
        }
    }

    /** Ferme le fichier de log */
    public static void close() {
        if (fileWriter != null) {
            fileWriter.flush();
            fileWriter.close();
        }
    }

    /** Nettoie un string de ses regex */
    public static String clearAnsiRegex(String str) {
        return str.replaceAll(ANSI_REGEX, "");
    }


    /** Génère le prefix temporel */
    private static String getTimePrefix() {
        return "[" + LocalDateTime.now().format(DATE_FORMAT) + "]";
    }

    /** Génère le prefix du canal */
    private static String getCanalPrefix(LOG_CANAL canal) {
        return "[" + canal.name() + "]";
    }

    /** Génère le message formaté */
    private static String getFormatedMessage(LOG_CANAL canal, VERBOSE_LEVEL vLevel, String msg) {
        // Ne pas log si le niveau de verbose du canal n'est pas suffisant
        if (vLevel.ordinal() > CANAL_VERBOSE_LEVEL.get(canal).ordinal()) return null;

        final String prefix = getTimePrefix() + " " + getCanalPrefix(canal);

        return prefix + " " + msg;
    }

    private static String strArraytoString(String... strArray) {
        final StringBuilder builder = new StringBuilder();
        for (String i : strArray) builder.append(i);
        return builder.toString();
    }

    /** Log un message standard */
    public static void log(LOG_CANAL canal, VERBOSE_LEVEL vLevel, String... msg) {
        final String rMsg = getFormatedMessage(canal, vLevel, ITALIC + strArraytoString(msg));
        if (rMsg != null) {
            System.out.println(rMsg + RESET);
            writeToFile(rMsg);
        }
    }

    /** Log un message warning */
    public static void warn(LOG_CANAL canal, VERBOSE_LEVEL vLevel, String... msg) {
        final String prefix = BOLD + " [WARN] " + RESET + ITALIC + YELLOW;
        final String rMsg = getFormatedMessage(canal, vLevel, prefix + strArraytoString(msg));
        if (rMsg != null) {
            System.out.println(YELLOW + rMsg + RESET);
            writeToFile(rMsg);
        }
    }

    /** Log un message d'erreur*/
    public static void error(LOG_CANAL canal, VERBOSE_LEVEL vLevel, String... msg) {
        final String prefix = BOLD + " [ERROR] " + RESET + ITALIC + RED;
        final String rMsg = getFormatedMessage(canal, vLevel, prefix + strArraytoString(msg));
        if (rMsg != null) {
            System.out.println(RED + rMsg + RESET);
            writeToFile(rMsg);
        }
    }
    /** Log un message d'erreur avec son exception */
    public static void error(LOG_CANAL canal, VERBOSE_LEVEL vLevel, Exception e, String... msg) {
        error(canal, vLevel, strArraytoString(msg) + " : " + EOL + e.getMessage());
    }
    /** Log un message d'erreur fatal et quitte l'application */
    public static void fatal(String... msg) {
        error(LOG_CANAL.MAIN, VERBOSE_LEVEL.SILENT, msg);
        App.end();
    }

}
