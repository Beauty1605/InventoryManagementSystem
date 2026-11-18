import java.sql.*;
import java.util.Scanner;

public class InventoryManagementSystem {
    // Database credentials (Apne MySQL username/password ke mutabiq change karein)
    static final String DB_URL = "jdbc:mysql://localhost:3306/retail_db";
    static final String USER = "root";
    static final String PASS = "Beauty@001122"; // Apna password yahan daalein

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            System.out.println("Database connected successfully!\n");
            
            int choice;
            do {
                System.out.println("===== INVENTORY MANAGEMENT SYSTEM =====");
                System.out.println("1. Insert Product Records (a)");
                System.out.println("2. Conditional Retrieval and Sorting (b)");
                System.out.println("3. Update Product Stock (c)");
                System.out.println("4. Search and Modify Product Details (d)");
                System.out.println("5. Aggregate Query and Conditional Deletion (e)");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        insertProducts(conn, scanner);
                        break;
                    case 2:
                        retrieveProducts(conn, scanner);
                        break;
                    case 3:
                        updateStock(conn, scanner);
                        break;
                    case 4:
                        modifyPrice(conn, scanner);
                        break;
                    case 5:
                        aggregateAndDelete(conn, scanner);
                        break;
                    case 6:
                        System.out.println("Exiting application. Thank you!");
                        break;
                    default:
                        System.out.println("Invalid choice! Please try again.\n");
                }
            } while (choice != 6);

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        scanner.close();
    }

    // (a) Insert Product Records using PreparedStatement and handle duplicate IDs
    public static void insertProducts(Connection conn, Scanner scanner) {
        String sql = "INSERT INTO Product (product_id, product_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            System.out.print("How many products do you want to insert? (At least 3 recommended): ");
            int n = scanner.nextInt();

            for (int i = 0; i < n; i++) {
                System.out.println("\nEnter details for product " + (i + 1) + ":");
                System.out.print("Product ID: ");
                int id = scanner.nextInt();
                scanner.nextLine(); // consume newline
                System.out.print("Product Name: ");
                String name = scanner.nextLine();
                System.out.print("Category: ");
                String category = scanner.nextLine();
                System.out.print("Quantity: ");
                int qty = scanner.nextInt();
                System.out.print("Price: ");
                double price = scanner.nextDouble();

                pstmt.setInt(1, id);
                pstmt.setString(2, name);
                pstmt.setString(3, category);
                pstmt.setInt(4, qty);
                pstmt.setDouble(5, price);

                try {
                    pstmt.executeUpdate();
                    System.out.println("Product inserted successfully!");
                } catch (SQLException e) {
                    // Handling duplicate primary key error
                    if (e.getErrorCode() == 1062) { // MySQL error code for duplicate entry
                        System.out.println("Error: Product with ID " + id + " already exists!");
                    } else {
                        System.out.println("Insertion failed: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
        }
        System.out.println();
    }

    // (b) Conditional Retrieval and Sorting by price descending
    public static void retrieveProducts(Connection conn, Scanner scanner) {
        scanner.nextLine(); // consume newline
        System.out.print("Enter category to search: ");
        String cat = scanner.nextLine();

        String sql = "SELECT * FROM Product WHERE category = ? AND quantity < 20 ORDER BY price DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cat);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n--- Products in category '" + cat + "' with quantity < 20 (Sorted by Price DESC) ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("ID: " + rs.getInt("product_id") +
                                   ", Name: " + rs.getString("product_name") +
                                   ", Quantity: " + rs.getInt("quantity") +
                                   ", Price: " + rs.getDouble("price"));
            }
            if (!found) {
                System.out.println("No products found matching the criteria.");
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    // (c) Update Product Stock by adding to existing quantity
    public static void updateStock(Connection conn, Scanner scanner) {
        System.out.print("Enter Product ID to update stock: ");
        int id = scanner.nextInt();
        System.out.print("Enter quantity to add: ");
        int addQty = scanner.nextInt();

        // Check if product exists first
        String checkSql = "SELECT quantity FROM Product WHERE product_id = ?";
        String updateSql = "UPDATE Product SET quantity = quantity + ? WHERE product_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                updateStmt.setInt(1, addQty);
                updateStmt.setInt(2, id);
                updateStmt.executeUpdate();
                System.out.println("Stock updated successfully for Product ID " + id);
            } else {
                System.out.println("Error: Product with ID " + id + " does not exist!");
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    // (d) Search and Modify Product Details (price)
    public static void modifyPrice(Connection conn, Scanner scanner) {
        System.out.print("Enter Product ID: ");
        int id = scanner.nextInt();
        System.out.print("Enter new price: ");
        double newPrice = scanner.nextDouble();

        if (newPrice <= 0) {
            System.out.println("Error: New price must be greater than zero!");
            return;
        }

        String checkSql = "SELECT * FROM Product WHERE product_id = ?";
        String updateSql = "UPDATE Product SET price = ? WHERE product_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Product exists, update price
                updateStmt.setDouble(1, newPrice);
                updateStmt.setInt(2, id);
                updateStmt.executeUpdate();
                System.out.println("Price updated successfully!");

                // Display updated product details
                System.out.println("\n--- Updated Product Details ---");
                System.out.println("ID: " + rs.getInt("product_id"));
                System.out.println("Name: " + rs.getString("product_name"));
                System.out.println("Category: " + rs.getString("category"));
                System.out.println("Quantity: " + rs.getInt("quantity"));
                System.out.println("New Price: " + newPrice);
            } else {
                System.out.println("Error: Product with ID " + id + " does not exist!");
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }

    // (e) Aggregate Query and Conditional Deletion
    public static void aggregateAndDelete(Connection conn, Scanner scanner) {
        // Aggregate query for total inventory value
        String aggSql = "SELECT SUM(quantity * price) AS total_value FROM Product";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(aggSql)) {
            if (rs.next()) {
                double totalValue = rs.getDouble("total_value");
                System.out.println("\nTotal Inventory Value (SUM(quantity * price)): " + totalValue);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error calculating aggregate value: " + e.getMessage());
        }

        // Conditional deletion
        System.out.print("\nEnter Product ID to delete: ");
        int id = scanner.nextInt();

        String checkSql = "SELECT quantity FROM Product WHERE product_id = ?";
        String deleteSql = "DELETE FROM Product WHERE product_id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int qty = rs.getInt("quantity");
                if (qty == 0) {
                    deleteStmt.setInt(1, id);
                    deleteStmt.executeUpdate();
                    System.out.println("Product with ID " + id + " deleted successfully as its quantity is zero.");
                } else {
                    System.out.println("Deletion failed: Product quantity is " + qty + " (must be 0 to delete).");
                }
            } else {
                System.out.println("Error: Product with ID " + id + " does not exist!");
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        System.out.println();
    }
}