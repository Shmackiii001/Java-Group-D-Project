package mark;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JOptionPane;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class App {
    private static final String url = "jdbc:mysql://localhost:3306/test_db";
    private static final String user = "root";
    private static final String password = "";

    private static Connection connection; // Class-level variable

    public static void main(String[] args) throws IOException {
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            while (true) {
                String[] options = { "Insert", "Select", "Update", "Delete", "Collect Money", "Exit" };
                int choice = JOptionPane.showOptionDialog(null, "Choose an operation:", "Database Operations",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

                switch (choice) {
                    case 0:
                        insertUser(connection);
                        break;
                    case 1:
                        selectUsers(connection);
                        break;
                    case 2:
                        updateUser(connection);
                        break;
                    case 3:
                        deleteUser(connection);
                        break;
                    case 4:
                        updateUserMoney(connection);
                        break;
                    case 5:
                        return;
                    default:
                        JOptionPane.showMessageDialog(null, "Invalid choice. Try again.");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Connection Failed: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void insertUser(Connection connection) {
        try {
            String name = JOptionPane.showInputDialog("Enter name:");
            String email = JOptionPane.showInputDialog("Enter email:");
            String sql = "INSERT INTO users (name, email) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                statement.setString(2, email);
                statement.executeUpdate();
                JOptionPane.showMessageDialog(null, "User inserted successfully.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Insertion Failed: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void selectUsers(Connection connection) {
        try {
            String sql = "SELECT * FROM users";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {
                StringBuilder result = new StringBuilder();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    String email = resultSet.getString("email");
                    double amount = resultSet.getDouble("amount");
                    result.append("ID: ").append(id).append(", Name: ").append(name).append(", Email: ").append(email)
                            .append(", Amount: ").append(amount).append("\n");
                }
                JOptionPane.showMessageDialog(null, result.length() > 0 ? result.toString() : "No users found.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Selection Failed: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void updateUser(Connection connection) {
        try {
            String idStr = JOptionPane.showInputDialog("Enter user ID to update:");
            int id = Integer.parseInt(idStr);
            String name = JOptionPane.showInputDialog("Enter new name:");
            String email = JOptionPane.showInputDialog("Enter new email:");
            String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                statement.setString(2, email);
                statement.setInt(3, id);
                statement.executeUpdate();
                JOptionPane.showMessageDialog(null, "User updated successfully.");
            }
        } catch (NumberFormatException | SQLException e) {
            JOptionPane.showMessageDialog(null, "Update Failed: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void deleteUser(Connection connection) {
        try {
            String idStr = JOptionPane.showInputDialog("Enter user ID to delete:");
            int id = Integer.parseInt(idStr);
            String sql = "DELETE FROM users WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                statement.executeUpdate();
                JOptionPane.showMessageDialog(null, "User deleted successfully.");
            }
        } catch (NumberFormatException | SQLException e) {
            JOptionPane.showMessageDialog(null, "Deletion Failed: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void updateUserMoney(Connection connection) {
        OkHttpClient client = new OkHttpClient();

        String phoneNumber = JOptionPane.showInputDialog(null, "Enter your phone number:");
        double amount = Double.parseDouble(JOptionPane.showInputDialog(null, "Enter the amount:"));

        try {
            // Step 1: Generate OAuth Token
            Request authRequest = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials")
                    .method("GET", null)
                    .addHeader("Authorization",
                            "Basic SXNmSmdzQ1NQWVVvUVhTOXBSOWJvQlJKQ1JTTE5IWWJKTkRuWUs2cHFGaHFJQTFwOjdVZEVST3ZNNTZvRWZGZ2JUWDdBQXJHd291TjJ3R3U1aDBIc1ROYUN5cUZXMk9rOGY5MmduRkVtYjFkWVc4bnU=")
                    .build();

            Response authResponse = client.newCall(authRequest).execute();

            if (!authResponse.isSuccessful()) {
                System.out.println("Auth request failed: " + authResponse.code() + " " + authResponse.message());
                return;
            }

            String authResponseBody = authResponse.body().string();
            System.out.println("Auth Response: " + authResponseBody);

            // Extract the access_token and expires_in values
            String accessToken = extractValue(authResponseBody, "access_token");
            String expiresInStr = extractValue(authResponseBody, "expires_in");
           
            // Convert expires_in to an integer
            Integer expiresIn = Integer.parseInt(expiresInStr);

            System.out.println("Access Token: " + accessToken);
            System.out.println("Expires In: " + expiresIn);

            // Step 2: Make the STK Push request
            MediaType mediaType = MediaType.parse("application/json");
            String jsonBody = "{\n" + "    \"BusinessShortCode\": 174379,\n"
                    + "    \"Password\": \"MTc0Mzc5YmZiMjc5ZjlhYTliZGJjZjE1OGU5N2RkNzFhNDY3Y2QyZTBjODkzMDU5YjEwZjc4ZTZiNzJhZGExZWQyYzkxOTIwMjQwNjE5MTUwMzUx\",\n"
                    + "    \"Timestamp\": \"20240619150351\",\n" + "    \"TransactionType\": \"CustomerPayBillOnline\",\n"
                    + "    \"Amount\": " + amount + ",\n" + "    \"PartyA\": \"" + phoneNumber + "\",\n"
                    + "    \"PartyB\": 174379,\n" + "    \"PhoneNumber\": \"" + phoneNumber + "\",\n"
                    + "    \"CallBackURL\": \"https://mydomain.com/path\",\n"
                    + "    \"AccountReference\": \"CompanyXLTD\",\n" + "    \"TransactionDesc\": \"Payment of X\"\n"
                    + "}";
            RequestBody body = RequestBody.create(jsonBody, mediaType);

            // Use the extracted access token in STK Push request
            Request stkPushRequest = new Request.Builder()
                    .url("https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest").method("POST", body)
                    .addHeader("Content-Type", "application/json").addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            Response stkPushResponse = client.newCall(stkPushRequest).execute();

            if (stkPushResponse.isSuccessful()) {
                System.out.println("STK Push request successful:");
                System.out.println(stkPushResponse.body().string());
            } else {
                System.out.println("STK Push request failed: " + stkPushResponse.code() + " "
                        + stkPushResponse.message());
            }
            updateUser2(connection);
        } catch (IOException e) {
            System.out.println("IOException occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Utility method to extract values from JSON-like string
    private static String extractValue(String jsonString, String key) {
        String keyPattern = "\"" + key + "\": \"";
        int startIndex = jsonString.indexOf(keyPattern);
        if (startIndex == -1) {
            System.out.println("Key '" + key + "' not found in the response.");
            return "";
        }
        startIndex += keyPattern.length();
        int endIndex = jsonString.indexOf("\"", startIndex);
        if (endIndex == -1) {
            System.out.println("End of value not found for key '" + key + "'.");
            return "";
        }

       
        return jsonString.substring(startIndex, endIndex);
        
    } 

    // Additional method for another user operation
    private static void updateUser2(Connection connection) {
        String[] options2 = { "Old User", "New User", "Exit" };
        int choice2 = JOptionPane.showOptionDialog(null, "Choose another operation:", "Additional Operations",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options2, options2[0]);
                
        switch (choice2) {
            case 0:
                // Perform option 1 operation
               try {
            String idStr = JOptionPane.showInputDialog("Enter user ID to update amount:");
            int id = Integer.parseInt(idStr);
            String amountStr = JOptionPane.showInputDialog("Enter new amount:");
            double amount = Double.parseDouble(amountStr); // Assuming amount is a double

            String sql = "UPDATE users SET amount = ? WHERE id = ?";

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, amount);
                statement.setInt(2, id);

                int rowsUpdated = statement.executeUpdate();
                if (rowsUpdated > 0) {
                    JOptionPane.showMessageDialog(null, "Amount updated successfully for user ID " + id);
                } else {
                    JOptionPane.showMessageDialog(null, "No user found with ID " + id);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "SQL Error: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid input format.");
        } finally {
            // Close your database connection here if necessary
        }
    
                break;
            case 1:
                // Perform option 2 operation
                try {
                    String name = JOptionPane.showInputDialog("Enter name:");
                    String email = JOptionPane.showInputDialog("Enter email:");
                    String moneystr = JOptionPane.showInputDialog("Enter amount of money:");
                    Double amount=Double.parseDouble(moneystr);

                    String sql = "INSERT INTO users (name, email,amount) VALUES (?, ?,?)";
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, name);
                        statement.setString(2, email);
                        statement.setDouble(3, amount);

                        statement.executeUpdate();
                        JOptionPane.showMessageDialog(null, "User inserted successfully.");
                    }
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(null, "Insertion Failed: " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                break;
            case 2:
                // Perform option 3 operation
                return;
                
            default:
                JOptionPane.showMessageDialog(null, "Invalid choice. Try again.");
        }
       
    }
}
