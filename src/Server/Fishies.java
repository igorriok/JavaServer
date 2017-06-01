package Server;

import java.sql.*;

import static Server.Deal.Entries;

public class Fishies extends Thread {

    Connection conn;

    Fishies() {
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
    private synchronized void insert(String token, int points) {

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
    
    synchronized void updatePoints(int ID, int points) {
        String sql = "SELECT " + Entries.PET_POINTS + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_ID + " = ?;";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, ID);
            ResultSet rs = pstmt.executeQuery();
            points = points + rs.getInt(Entries.PET_POINTS);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        sql = "UPDATE " + Entries.TABLE_NAME + " SET " + Entries.PET_POINTS + " = ? WHERE " + Entries.PET_ID + " = ?;";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, points);
            pstmt.setInt(2, ID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    synchronized int getID(String token) {
        int ID = 0;
        String sql = "SELECT * " + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_TOKEN + " = ?;";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, token);
            ResultSet rs = pstmt.executeQuery();
            ID = rs.getInt(Entries.PET_ID);
        } catch (SQLException e) {
            System.out.println("No such record " + e.getMessage());
            insert(token, 0);
            try {
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, token);
                ResultSet rs = pstmt.executeQuery();
                ID = rs.getInt(Entries.PET_ID);
            } catch (SQLException error) {
                System.out.println("Record hasn't been added " + error.getMessage());
            }
        }
        return ID;
    }

    synchronized int getPointsByID(int ID) {
        int points = 0;
        String sql = "SELECT * " + " FROM " + Entries.TABLE_NAME + " WHERE " + Entries.PET_ID + " = ?;";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, ID);
            ResultSet rs = pstmt.executeQuery();
            points = rs.getInt(Entries.PET_POINTS);
        } catch (SQLException e) {
            System.out.println("No such record " + e.getMessage());
        }
        return points;
    }
}
