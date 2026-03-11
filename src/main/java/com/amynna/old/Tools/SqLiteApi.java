package com.amynna.old.Tools;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqLiteApi {


    private final Connection connection;

    // Constructeur privé pour le Singleton
    public SqLiteApi(File dbFile) throws SQLException {

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        connection = DriverManager.getConnection(url);
        // Optionnel : Activer les clés étrangères
        connection.createStatement().execute("PRAGMA foreign_keys = ON;");

    }

    /**
     * Pour INSERT, UPDATE, DELETE
     * @param sql La requête avec des '?'
     * @param params Les valeurs à injecter
     */
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.error("Erreur SQL (Update) : " + e.getMessage());
            return -1;
        }
    }

    /**
     * Exécute une requête SELECT et retourne les résultats sous forme de Liste de Maps.
     * Chaque Map représente une ligne : <Nom de la colonne, Valeur en String>
     */
    public List<Map<String, String>> selectToMapList(String sql, Object... params) {
        List<Map<String, String>> results = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {

            // Injection des paramètres (filtres WHERE, etc.)
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        // On récupère tout en String via getString()
                        // Si la valeur est NULL, SQLite JDBC retourne null
                        String value = rs.getString(i);
                        row.put(columnName, value != null ? value : "NULL");
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du SELECT : " + e.getMessage());
        }

        return results;
    }
}
