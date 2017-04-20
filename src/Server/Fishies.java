package Server;

import java.sql.*;

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
     * @param token received from app
     * @param points default is 0
     */
    public void insert(String token, int points) {

        String sql = "INSERT INTO " + Entries.TABLE_NAME + "(" + Entries.PET_TOKEN + "," + Entries.PET_POINTS + ") VALUES(?,?);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, token);
            pstmt.setDouble(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Cant add record " + e.getMessage());
        }
    }
    
    public void updatePoints(String token, int points) {
        String sql = "SELECT " + Entries.PET_POINTS + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_TOKEN + " = " + token + ";";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            points = points + rs.getInt(Entries.PET_POINTS);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        sql = "UPDATE " + Entries.TABLE_NAME + " SET " + Entries.PET_POINTS + " = " + points + " WHERE " + Entries.PET_TOKEN + " = " + token + ";";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public int[] getPoints(String token) {
        int[] points = new int[2];
        String sql = "SELECT " + Entries.PET_POINTS + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_TOKEN + " = " + token + ";";
        
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            points[0] = rs.getInt(Entries.PET_POINTS);
            points[1] = rs.getInt(Entries.PET_ID);
        } catch (SQLException e) {
            System.out.println("No such record " + e.getMessage());
        } finally {
            insert(token, 0);
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery();
                points[0] = rs.getInt(Entries.PET_POINTS);
                points[1] = rs.getInt(Entries.PET_ID);
            } catch (SQLException e) {
                System.out.println("Record hasn't been added " + e.getMessage());
            }
        }
        return points;
    }
    
    public Boolean check(String token) {
        
        String sql = "SELECT EXISTS (SELECT " + 1 + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_TOKEN + " = " + token + " LIMIT 1);";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.getInt(1) > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
        return false;
    }
}
