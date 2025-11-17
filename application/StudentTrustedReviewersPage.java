package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;

/**
 * <p> Title: Student Trusted Reviewers Page </p>
 *
 * <p> Description: This class provides the user interface for students to manage their trusted reviewers.
 * Students can view the list of reviewers they trust, update trust weights, remove trust relationships,
 * and navigate to reviewer profiles. This supports the trust-weighted review system by allowing students
 * to prioritize feedback from reviewers they trust. </p>
 *
 * <p> Copyright: © 2025 </p>
 * 
 *
 * @version 1.00        2025        Initial implementation of TP3 trusted reviewer management
 */
public class StudentTrustedReviewersPage {

    private final DatabaseHelper databaseHelper;
    private final User currentUser;

    /**
     * Initializes the page with the database helper and current user.
     *
     * @param databaseHelper The helper class for database operations.
     * @param currentUser The student currently logged in.
     */
    public StudentTrustedReviewersPage(DatabaseHelper databaseHelper, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
    }

    /**
     * Displays the trusted reviewers page.
     *
     * @param primaryStage The main application window.
     */
    public void show(Stage primaryStage) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Header label
        Label header = new Label("Your Trusted Reviewers");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox trustList = new VBox(10);

        try {
            // Get list of trusted reviewers from the database
            ArrayList<String> trustedReviewers = databaseHelper.getTrustedReviewers(currentUser.getUserName());

            if (trustedReviewers.isEmpty()) {
                trustList.getChildren().add(new Label("You haven't trusted any reviewers yet."));
            } else {
                // Display each trusted reviewer with options
                for (String reviewer : trustedReviewers) {
                    VBox reviewerBox = new VBox(5);
                    reviewerBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #ccc;");

                    Label nameLabel = new Label("Reviewer: " + reviewer);
                    nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

                    // Show current trust weight
                    int weight = databaseHelper.getTrustWeight(currentUser.getUserName(), reviewer);
                    Label weightLabel = new Label("Trust Weight: " + weight);

                    // Dropdown to update trust weight
                    ComboBox<Integer> weightDropdown = new ComboBox<>();
                    weightDropdown.getItems().addAll(1, 2, 3);
                    weightDropdown.setPromptText("Update weight");

                    Button updateButton = new Button("Update");
                    updateButton.setOnAction(e -> {
                        Integer newWeight = weightDropdown.getValue();
                        if (newWeight != null) {
                            try {
                                databaseHelper.updateStudentTrust(currentUser.getUserName(), reviewer, newWeight);
                                weightLabel.setText("Trust Weight: " + newWeight);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    // Button to remove trust relationship
                    Button removeButton = new Button("Remove Trust");
                    removeButton.setOnAction(e -> {
                        try {
                            databaseHelper.removeTrust(currentUser.getUserName(), reviewer);
                            show(primaryStage); // Refresh the page
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });

                    // Button to view reviewer profile
                    Button profileButton = new Button("View Profile");
                    profileButton.setOnAction(e -> {
                        ReviewerProfilePage page = new ReviewerProfilePage(databaseHelper, currentUser);
                        page.setReturnToTrusted(true); // Set flag to return here
                        page.show(primaryStage, reviewer, null);
                    });

                    reviewerBox.getChildren().addAll(nameLabel, weightLabel, weightDropdown, updateButton, removeButton, profileButton);
                    trustList.getChildren().add(reviewerBox);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // Back button to return to student home
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> new StudentHomePage(databaseHelper).show(primaryStage, currentUser));

        layout.getChildren().addAll(backButton, header, trustList);

        // Final scene setup
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Trusted Reviewers");
        primaryStage.show();
    }
}
