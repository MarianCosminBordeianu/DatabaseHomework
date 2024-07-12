package org.example;


import org.junit.jupiter.api.*;
import java.sql.*;

/**
 * Unit test for simple App.
 */
public class AppTest
{
    private static final String DB_URL = System.getenv("url");
    private static final String USER = System.getenv("user");
    private static final String PASS = System.getenv("pass");
    private Connection conn;

    @BeforeEach
    public void setUp() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
        conn.setAutoCommit(false);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (conn != null) {
            conn.rollback();
            conn.close();
        }
    }

    @Test
    public void testInsertData() throws SQLException {
        String insertAccommodationSQL = "INSERT INTO accommodation (type, bed_type, max_guests, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertAccommodationSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Hotel Room");
            pstmt.setString(2, "Queen");
            pstmt.setInt(3, 2);
            pstmt.setString(4, "A cozy hotel room with a queen-size bed.");
            pstmt.executeUpdate();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            generatedKeys.next();
            int accommodationId = generatedKeys.getInt(1);

            String insertRoomFairSQL = "INSERT INTO room_fair (value, season) VALUES (?, ?)";
            try (PreparedStatement pstmt2 = conn.prepareStatement(insertRoomFairSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt2.setDouble(1, 150.00);
                pstmt2.setString(2, "Summer");
                pstmt2.executeUpdate();

                generatedKeys = pstmt2.getGeneratedKeys();
                generatedKeys.next();
                int roomFairId = generatedKeys.getInt(1);

                String insertRelationSQL = "INSERT INTO accommodation_room_fair_relation (accommodation_id, room_fair_id) VALUES (?, ?)";
                try (PreparedStatement pstmt3 = conn.prepareStatement(insertRelationSQL)) {
                    pstmt3.setInt(1, accommodationId);
                    pstmt3.setInt(2, roomFairId);
                    pstmt3.executeUpdate();
                }
            }
        }
    }

    @Test
    public void testQueryPrices() throws SQLException {
        testInsertData();

        String selectSQL = "SELECT a.type, a.description, rf.value, rf.season " +
                "FROM accommodation a " +
                "JOIN accommodation_room_fair_relation arfr ON a.id = arfr.accommodation_id " +
                "JOIN room_fair rf ON rf.id = arfr.room_fair_id";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSQL);
             ResultSet rs = pstmt.executeQuery()) {
            Assertions.assertTrue(rs.next(), "No data returned");

            do {
                String type = rs.getString("type");
                String description = rs.getString("description");
                double value = rs.getDouble("value");
                String season = rs.getString("season");
                System.out.println("Accommodation: " + type + ", Description: " + description + ", Price: " + value + ", Season: " + season);
            } while (rs.next());
        }
    }
}
