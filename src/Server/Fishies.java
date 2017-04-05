package Server;

import java.sql.*;

import static Server.Deal.Entries;

public class Fishies extends Thread {

    Connection conn;

    public Fishies() {
    }

    public void run() {

        try {
            Connection conn = DriverManager.getConnection(Entries.url);
            Statement stmt = conn.createStatement();
            // create a new table
            stmt.execute(Entries.sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Insert a new row into the warehouses table
     * @param name
     * @param capacity
     */
    public void insert(String name, double capacity) {

        String sql = "INSERT INTO " + Entries.TABLE_NAME + "(" + Entries.PET_TOKEN + "," + Entries.PET_POINTS + ") VALUES(?,?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setDouble(2, capacity);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
