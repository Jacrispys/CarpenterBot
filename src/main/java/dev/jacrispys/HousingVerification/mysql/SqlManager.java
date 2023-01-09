package dev.jacrispys.HousingVerification.mysql;

import dev.jacrispys.HousingVerification.SecretData;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlManager {

    protected SqlManager() {
        connectionManager();
        thread.start();
    }

    private static Connection connection;
    private static SqlManager INSTANCE;

    public static SqlManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SqlManager();
        }

        return INSTANCE;
    }

    public Connection getSqlConnection() {
        return connection;
    }


    private static Connection resetConnection(String dataBase) throws SQLException {
        try {
            String userName = "Jacrispys";
            String db_password = SecretData.getDataBasePass();

            String url = "jdbc:mysql://" + SecretData.getDBHost() + ":3306/" + dataBase + "?autoReconnect=true";
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            connection = DriverManager.getConnection(url, userName, db_password);
            return connection;


        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Could not connect to the given database!");
        }
    }
    private static Thread thread;

    private static void connectionManager() {
        thread = new Thread(() -> {
            while (true) {
                try {
                    System.out.println("Enabling Database...");
                    connection = resetConnection("mc_discord");
                    System.out.println("Enabled!");
                    Thread.sleep(3600 * 1000);
                } catch (SQLException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
