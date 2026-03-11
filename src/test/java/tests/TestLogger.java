package tests;

import com.amynna.OriginLauncher.output.Logger;
import com.amynna.OriginLauncher.output.Logger.LOG_CANAL;
import com.amynna.OriginLauncher.output.Logger.VERBOSE_LEVEL;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe {@link Logger}.
 */
class TestLogger {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream capturedOut;

    // ========================
    //  SETUP / TEARDOWN
    // ========================

    @BeforeEach
    void setUp() throws Exception {
        // Capture la sortie console
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));

        // Réinitialise le fileWriter à null (pas d'écriture fichier par défaut)
        setFileWriter(null);

        // Réinitialise tous les canaux en STANDARD
        resetCanalVerboseLevels(VERBOSE_LEVEL.STANDARD);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        Logger.close();
    }

    // ========================
    //  TESTS DES ENUMS
    // ========================

    @Test
    @DisplayName("VERBOSE_LEVEL contient les 4 niveaux dans le bon ordre")
    void testVerboseLevelValues() {
        VERBOSE_LEVEL[] levels = VERBOSE_LEVEL.values();
        assertEquals(4, levels.length);
        assertEquals(VERBOSE_LEVEL.SILENT, levels[0]);
        assertEquals(VERBOSE_LEVEL.STANDARD, levels[1]);
        assertEquals(VERBOSE_LEVEL.VERBOSE, levels[2]);
        assertEquals(VERBOSE_LEVEL.VERY_VERBOSE, levels[3]);
    }

    @Test
    @DisplayName("VERBOSE_LEVEL ordinal() respecte la hiérarchie croissante")
    void testVerboseLevelOrdinalOrder() {
        assertTrue(VERBOSE_LEVEL.SILENT.ordinal() < VERBOSE_LEVEL.STANDARD.ordinal());
        assertTrue(VERBOSE_LEVEL.STANDARD.ordinal() < VERBOSE_LEVEL.VERBOSE.ordinal());
        assertTrue(VERBOSE_LEVEL.VERBOSE.ordinal() < VERBOSE_LEVEL.VERY_VERBOSE.ordinal());
    }

    // ========================
    //  TESTS DE clearAnsiRegex()
    // ========================

    @Test
    @DisplayName("clearAnsiRegex() supprime tous les codes ANSI d'une chaîne")
    void testClearAnsiRegexRemovesAllCodes() {
        String input = Logger.RED + "Erreur" + Logger.RESET + " " + Logger.BOLD + "critique" + Logger.RESET;
        assertEquals("Erreur critique", Logger.clearAnsiRegex(input));
    }

    @Test
    @DisplayName("clearAnsiRegex() ne modifie pas une chaîne sans codes ANSI")
    void testClearAnsiRegexKeepsCleanString() {
        String input = "Message sans couleur";
        assertEquals("Message sans couleur", Logger.clearAnsiRegex(input));
    }

    @Test
    @DisplayName("clearAnsiRegex() gère une chaîne vide")
    void testClearAnsiRegexEmptyString() {
        assertEquals("", Logger.clearAnsiRegex(""));
    }

    @Test
    @DisplayName("clearAnsiRegex() supprime les codes de style (bold, italic, underline)")
    void testClearAnsiRegexRemovesStyleCodes() {
        String input = Logger.BOLD + Logger.ITALIC + Logger.UNDERLINE + "Texte stylé" + Logger.RESET;
        assertEquals("Texte stylé", Logger.clearAnsiRegex(input));
    }

    @Test
    @DisplayName("clearAnsiRegex() supprime les codes imbriqués (couleur + style)")
    void testClearAnsiRegexRemovesNestedCodes() {
        String input = Logger.BOLD + Logger.RED + "[ERROR]" + Logger.RESET + Logger.ITALIC + Logger.YELLOW + " warning" + Logger.RESET;
        assertEquals("[ERROR] warning", Logger.clearAnsiRegex(input));
    }

    // ========================
    //  TESTS DE LOG (sortie console)
    // ========================

    @Test
    @DisplayName("log() affiche un message sur la sortie standard")
    void testLogOutputsToConsole() {
        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Hello World");

        String output = capturedOut.toString();
        assertTrue(output.contains("Hello World"), "Le message devrait contenir 'Hello World'");
        assertTrue(output.contains("[MAIN]"), "Le message devrait contenir le canal [MAIN]");
    }

    @Test
    @DisplayName("log() inclut le timestamp au format dd/MM/yyyy HH:mm:ss")
    void testLogContainsTimestamp() {
        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "test timestamp");

        String output = Logger.clearAnsiRegex(capturedOut.toString()).trim();
        assertTrue(output.matches(".*\\[\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}].*"),
                "Le message devrait contenir un timestamp au format [dd/MM/yyyy HH:mm:ss]");
    }

    @Test
    @DisplayName("warn() affiche un message avec le préfixe [WARN]")
    void testWarnOutputsWithWarnPrefix() {
        Logger.warn(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Attention");

        String output = capturedOut.toString();
        assertTrue(output.contains("[WARN]"), "Le message devrait contenir [WARN]");
        assertTrue(output.contains("Attention"), "Le message devrait contenir 'Attention'");
    }

    @Test
    @DisplayName("error() affiche un message avec le préfixe [ERROR]")
    void testErrorOutputsWithErrorPrefix() {
        Logger.error(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Erreur critique");

        String output = capturedOut.toString();
        assertTrue(output.contains("[ERROR]"), "Le message devrait contenir [ERROR]");
        assertTrue(output.contains("Erreur critique"), "Le message devrait contenir 'Erreur critique'");
    }

    @Test
    @DisplayName("error() avec exception inclut le message de l'exception")
    void testErrorWithExceptionContainsExceptionMessage() {
        Exception ex = new RuntimeException("NPE simulée");
        Logger.error(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, ex, "Erreur inattendue");

        String output = capturedOut.toString();
        assertTrue(output.contains("NPE simulée"), "Le message devrait contenir le message de l'exception");
        assertTrue(output.contains("Erreur inattendue"), "Le message devrait contenir le message custom");
    }

    // ========================
    //  TESTS DE FILTRAGE PAR VERBOSE LEVEL
    // ========================

    @Test
    @DisplayName("Un message VERBOSE n'est PAS affiché si le canal est en STANDARD")
    void testVerboseMessageFilteredWhenCanalIsStandard() {
        resetCanalVerboseLevels(VERBOSE_LEVEL.STANDARD);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.VERBOSE, "Message verbeux");

        String output = capturedOut.toString();
        assertFalse(output.contains("Message verbeux"),
                "Un message VERBOSE ne devrait pas être affiché quand le canal est en STANDARD");
    }

    @Test
    @DisplayName("Un message VERBOSE EST affiché si le canal est en VERBOSE")
    void testVerboseMessageShownWhenCanalIsVerbose() {
        resetCanalVerboseLevels(VERBOSE_LEVEL.VERBOSE);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.VERBOSE, "Message verbeux");

        String output = capturedOut.toString();
        assertTrue(output.contains("Message verbeux"),
                "Un message VERBOSE devrait être affiché quand le canal est en VERBOSE");
    }

    @Test
    @DisplayName("Un message VERBOSE EST affiché si le canal est en VERY_VERBOSE")
    void testVerboseMessageShownWhenCanalIsVeryVerbose() {
        resetCanalVerboseLevels(VERBOSE_LEVEL.VERY_VERBOSE);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.VERBOSE, "Message verbeux");

        String output = capturedOut.toString();
        assertTrue(output.contains("Message verbeux"),
                "Un message VERBOSE devrait être affiché quand le canal est en VERY_VERBOSE");
    }

    @Test
    @DisplayName("Un message STANDARD EST affiché si le canal est en STANDARD")
    void testStandardMessageShownWhenCanalIsStandard() {
        resetCanalVerboseLevels(VERBOSE_LEVEL.STANDARD);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Message standard");

        String output = capturedOut.toString();
        assertTrue(output.contains("Message standard"));
    }

    @Test
    @DisplayName("Aucun message n'est affiché si le canal est en SILENT")
    void testNoMessageWhenCanalIsSilent() {
        resetCanalVerboseLevels(VERBOSE_LEVEL.SILENT);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Message caché");
        Logger.warn(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Warning caché");
        Logger.error(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Error cachée");

        String output = capturedOut.toString();
        assertTrue(output.isEmpty(), "Aucun message ne devrait être affiché quand le canal est SILENT");
    }

    @Test
    @DisplayName("Le filtrage est indépendant par canal")
    void testFilteringIsPerCanal() throws Exception {
        setCanalVerboseLevel(LOG_CANAL.MAIN, VERBOSE_LEVEL.VERY_VERBOSE);
        setCanalVerboseLevel(LOG_CANAL.GRAPHIC, VERBOSE_LEVEL.SILENT);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.VERY_VERBOSE, "Visible");
        Logger.log(LOG_CANAL.GRAPHIC, VERBOSE_LEVEL.STANDARD, "Invisible");

        String output = capturedOut.toString();
        assertTrue(output.contains("Visible"), "Le message MAIN devrait être visible");
        assertFalse(output.contains("Invisible"), "Le message GRAPHIC devrait être filtré");
    }

    // ========================
    //  TESTS DES CANAUX DANS LE MESSAGE
    // ========================

    @Test
    @DisplayName("Le canal GRAPHIC apparaît dans le message formaté")
    void testGraphicCanalInOutput() {
        Logger.log(LOG_CANAL.GRAPHIC, VERBOSE_LEVEL.STANDARD, "Interface chargée");

        String output = capturedOut.toString();
        assertTrue(output.contains("[GRAPHIC]"));
    }

    @Test
    @DisplayName("Le canal FILES apparaît dans le message formaté")
    void testFilesCanalInOutput() {
        Logger.log(LOG_CANAL.FILES, VERBOSE_LEVEL.STANDARD, "Fichier créé");

        String output = capturedOut.toString();
        assertTrue(output.contains("[FILES]"));
    }

    // ========================
    //  TESTS ÉCRITURE FICHIER
    // ========================

    @Test
    @DisplayName("Les logs sont écrites dans le fichier sans codes ANSI")
    void testWriteToFileStripsAnsi(@TempDir Path tempDir) throws Exception {
        // Prépare un PrintWriter pointant vers un fichier temporaire
        Path logFile = tempDir.resolve("test.log");
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(logFile.toFile())), true);
        setFileWriter(pw);

        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Test fichier");

        pw.flush();
        pw.close();

        // Vérifie le contenu du fichier
        List<String> lines = Files.readAllLines(logFile);
        assertFalse(lines.isEmpty(), "Le fichier de log ne devrait pas être vide");

        String fileLine = lines.get(0);
        assertFalse(fileLine.contains("\u001B"), "Le fichier ne devrait pas contenir de codes ANSI");
        assertTrue(fileLine.contains("[MAIN]"), "Le fichier devrait contenir le canal");
        assertTrue(fileLine.contains("Test fichier"), "Le fichier devrait contenir le message");
    }

    @Test
    @DisplayName("warn() et error() sont aussi écrits dans le fichier")
    void testWarnAndErrorWrittenToFile(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("test.log");
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(logFile.toFile())), true);
        setFileWriter(pw);

        Logger.warn(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Un warning");
        Logger.error(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Une erreur");

        pw.flush();
        pw.close();

        List<String> lines = Files.readAllLines(logFile);
        assertEquals(2, lines.size(), "Le fichier devrait contenir 2 lignes");
        assertTrue(lines.get(0).contains("[WARN]"));
        assertTrue(lines.get(1).contains("[ERROR]"));
    }

    @Test
    @DisplayName("Aucune exception si le fileWriter est null (pas d'init)")
    void testNoExceptionWhenFileWriterIsNull() {
        assertDoesNotThrow(() -> {
            Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Sans fichier");
        });
    }

    @Test
    @DisplayName("close() ne lève pas d'exception si fileWriter est null")
    void testCloseWithNullFileWriter() throws Exception {
        setFileWriter(null);
        assertDoesNotThrow(Logger::close);
    }

    // ========================
    //  TESTS VARARGS (String...)
    // ========================

    @Test
    @DisplayName("log() accepte plusieurs arguments String")
    void testLogWithMultipleStringArgs() {
        Logger.log(LOG_CANAL.MAIN, VERBOSE_LEVEL.STANDARD, "Partie 1", " Partie 2", " Partie 3");

        String output = capturedOut.toString();
        assertTrue(output.contains("Partie 1"), "Devrait contenir le premier argument");
        assertTrue(output.contains("Partie 2"), "Devrait contenir le deuxième argument");
        assertTrue(output.contains("Partie 3"), "Devrait contenir le troisième argument");
    }

    // ========================
    //  UTILITAIRES (réflexion)
    // ========================


    /** Définit le fileWriter via réflexion */
    private static void setFileWriter(PrintWriter writer) {
        try {
            Field field = Logger.class.getDeclaredField("fileWriter");
            field.setAccessible(true);
            field.set(null, writer);
        } catch (Exception e) {
            fail("Impossible de modifier fileWriter par réflexion : " + e.getMessage());
        }
    }

    /** Réinitialise tous les canaux à un même niveau de verbose */
    @SuppressWarnings("unchecked")
    private static void resetCanalVerboseLevels(VERBOSE_LEVEL level) {
        try {
            Field field = Logger.class.getDeclaredField("CANAL_VERBOSE_LEVEL");
            field.setAccessible(true);
            Map<LOG_CANAL, VERBOSE_LEVEL> map = (Map<LOG_CANAL, VERBOSE_LEVEL>) field.get(null);
            for (LOG_CANAL canal : LOG_CANAL.values()) {
                map.put(canal, level);
            }
        } catch (Exception e) {
            fail("Impossible de modifier CANAL_VERBOSE_LEVEL par réflexion : " + e.getMessage());
        }
    }

    /** Définit le niveau de verbose pour un canal spécifique */
    @SuppressWarnings("unchecked")
    private static void setCanalVerboseLevel(LOG_CANAL canal, VERBOSE_LEVEL level) {
        try {
            Field field = Logger.class.getDeclaredField("CANAL_VERBOSE_LEVEL");
            field.setAccessible(true);
            Map<LOG_CANAL, VERBOSE_LEVEL> map = (Map<LOG_CANAL, VERBOSE_LEVEL>) field.get(null);
            map.put(canal, level);
        } catch (Exception e) {
            fail("Impossible de modifier CANAL_VERBOSE_LEVEL par réflexion : " + e.getMessage());
        }
    }
}