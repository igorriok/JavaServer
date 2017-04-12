package Server;

import java.sql.*;
import java.util.ArrayList;

import static Server.Deal.Entries;

public class Fishies extends Thread {

    Connection conn;

    public Fishies() {
    }

    public void run() {

        try {
            conn = DriverManager.getConnection(Entries.url);
            Statement stmt = conn.createStatement();
            // create a new table
            stmt.execute(Entries.sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Insert a new row into the warehouses table
     * @param token
     * @param points
     */
    public void insert(String token, int points) {

        String sql = "INSERT INTO " + Entries.TABLE_NAME + "(" + Entries.PET_TOKEN + "," + Entries.PET_POINTS + ") VALUES(?,?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, token);
            pstmt.setDouble(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList getPoints(String token) {
        ArrayList<> points = new ArrayList<>;
        String sql = "SELECT" + Entries.PET_POINTS + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_TOKEN + " = " + token;
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            points.add(rs.getInt(Entries.PET_POINTS));
            points.add(rs.getInt(Entries.PET_ID));
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return points;
    }
}
