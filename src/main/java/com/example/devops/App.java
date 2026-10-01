package com.example.devops;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class App {

    private static Connection connection;

    public static String message() {
        return "DevOps Pipeline works!";
    }

    public static void main(String[] args) throws Exception {

        // Base SQLite uniquement pour le laboratoire SQLMap
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");

        try (Statement statement = connection.createStatement()) {

            statement.execute(
                "CREATE TABLE items (" +
                "id INTEGER PRIMARY KEY, " +
                "name TEXT)"
            );

            statement.execute(
                "INSERT INTO items(id, name) VALUES " +
                "(1, 'Laptop'), " +
                "(2, 'Phone'), " +
                "(3, 'Tablet')"
            );
        }

        HttpServer server =
                HttpServer.create(new InetSocketAddress(8085), 0);

        // Endpoint principal
        server.createContext("/", exchange -> {

            String response = message();

            exchange.sendResponseHeaders(
                    200,
                    response.getBytes().length
            );

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        // Health check
        server.createContext("/health", exchange -> {

            String response = "OK";

            exchange.sendResponseHeaders(
                    200,
                    response.getBytes().length
            );

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        // Endpoint volontairement vulnérable pour SQLMap
        server.createContext("/item", exchange -> {

            String query = exchange.getRequestURI().getRawQuery();

            String id = "1";

            if (query != null && query.startsWith("id=")) {
                id = URLDecoder.decode(
                        query.substring(3),
                        StandardCharsets.UTF_8
                );
            }

            String response;

            try {

                /*
                 * Vulnérabilité volontaire pour le LAB.
                 *
                 * Ne jamais utiliser cette méthode en production.
                 */
                String sql =
                        "SELECT name FROM items WHERE id = " + id;

                System.out.println("SQL query: " + sql);

                try (Statement statement =
                             connection.createStatement();

                     ResultSet result =
                             statement.executeQuery(sql)) {

                    if (result.next()) {
                        response = result.getString("name");
                    } else {
                        response = "Item not found";
                    }
                }

            } catch (Exception e) {

                response = "SQL Error: " + e.getMessage();
            }

            exchange.sendResponseHeaders(
                    200,
                    response.getBytes().length
            );

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });

        server.start();

        System.out.println(message());
        System.out.println("HTTP server started on port 8085");
        System.out.println(
                "SQLMap lab endpoint: http://localhost:8085/item?id=1"
        );
    }
}
