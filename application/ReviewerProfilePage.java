package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.*;


/**
 * <p> Title: Reviewer Profile Page </p>
 *
 * <p> Description: This class provides the user interface for displaying a reviewer's profile.
 * It supports both student and reviewer roles. Students can view reviews written by a reviewer,
 * assign or remove trust weights, send private messages, and view review history. Reviewers can
 * navigate to their own Q&A forum and view their review contributions. </p>
 *
 * <p> Copyright: © 2025 </p>
 *
 *
 * @version 1.00        2025        Initial implementation of TP3 reviewer profile logic
 */
public class ReviewerProfilePage {

    private final DatabaseHelper databaseHelper;
    private final User currentUser;
    private boolean returnToTrusted = false;

    /**
     * Initializes the ReviewerProfilePage with the database helper and current user.
     *
     * @param databaseHelper The helper class for database operations.
     * @param currentUser The user currently logged in.
     */
    public ReviewerProfilePage(DatabaseHelper databaseHelper, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
    }

    /**
     * Sets a flag to determine if the back button should return to the trusted reviewers page.
     *
     * @param value True if returning to trusted reviewers, false otherwise.
     */
    public void setReturnToTrusted(boolean value) {
        this.returnToTrusted = value;
    }

    /**
     * Displays the reviewer profile page.
     *
     * @param primaryStage The main application window.
     * @param reviewerUsername The username of the reviewer being viewed.
     * @param originQuestion The question that led to this profile view (optional).
     */
    public void show(Stage primaryStage, String reviewerUsername, Question originQuestion) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Header showing reviewer username
        Label header = new Label("Reviewer Profile: " + reviewerUsername);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        layout.getChildren().add(header);

        // Back button logic depending on navigation context
        if (returnToTrusted) {
            Button backToTrustedButton = new Button("Back to Trusted Reviewers");
            backToTrustedButton.setOnAction(e -> {
                new StudentTrustedReviewersPage(databaseHelper, currentUser).show(primaryStage);
            });
            layout.getChildren().add(backToTrustedButton);
        } else if (originQuestion != null) {
            Button backToReviewsButton = new Button("Back to Answer Reviews");
            backToReviewsButton.setOnAction(e -> {
                new AnswerReviewsPage(databaseHelper, currentUser).show(primaryStage, originQuestion);
            });
            layout.getChildren().add(backToReviewsButton);
        }

        // If viewing own profile as reviewer
        if (currentUser.getUserName().equals(reviewerUsername)) {
            Label roleLabel = new Label("Your Roles: " + currentUser.getRole());
            roleLabel.setStyle("-fx-font-size: 13px;");
            layout.getChildren().add(roleLabel);

            Button qaButton = new Button("Go to Q&A Forum");
            qaButton.setOnAction(e -> new QuestionsPage(databaseHelper, currentUser, "reviewer").show(primaryStage));
            layout.getChildren().add(qaButton);

            Button backButton = new Button("Back to Role Selection");
            backButton.setOnAction(e -> new RoleSelectionPage(databaseHelper).show(primaryStage, currentUser));
            layout.getChildren().add(backButton);

        } else {
            // Trust weight section for students viewing a reviewer
            Label trustLabel = new Label();
            ComboBox<Integer> trustDropdown = new ComboBox<>();
            trustDropdown.getItems().addAll(1, 2, 3);
            trustDropdown.setPromptText("Set trust weight");

            Button updateTrustButton = new Button("Update Trust");
            Button removeTrustButton = new Button("Remove Trust");

            try {
                int currentWeight = databaseHelper.getTrustWeight(currentUser.getUserName(), reviewerUsername);
                trustLabel.setText(currentWeight > 0 ? "Current Trust Weight: " + currentWeight : "Reviewer not trusted yet");

                // Update trust weight
                updateTrustButton.setOnAction(e -> {
                    Integer selected = trustDropdown.getValue();
                    if (selected != null) {
                        try {
                            databaseHelper.updateStudentTrust(currentUser.getUserName(), reviewerUsername, selected);
                            trustLabel.setText("Current Trust Weight: " + selected);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

                // Remove trust relationship
                removeTrustButton.setOnAction(e -> {
                    try {
                        databaseHelper.removeTrust(currentUser.getUserName(), reviewerUsername);
                        trustLabel.setText("Reviewer not trusted yet");
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            // Button to send a private message to the reviewer
            Button messageButton = new Button("Message Reviewer");
            messageButton.setOnAction(e -> {
                try {
                    ArrayList<User> users = databaseHelper.getAllUsers();
                    for (User u : users) {
                        if (u.getUserName().equals(reviewerUsername)) {
                            new StudentPrivateMessagePage(databaseHelper, currentUser).show(primaryStage, null, u);
                            break;
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            layout.getChildren().addAll(trustLabel, trustDropdown, updateTrustButton, removeTrustButton, messageButton);
        }

        // Display reviews written by the reviewer, grouped by answer
        Map<Integer, List<Review>> reviewsByAnswer = new HashMap<>();
        try {
            ArrayList<Review> allReviews = databaseHelper.getAllUserReviews(reviewerUsername);
            for (Review r : allReviews) {
                reviewsByAnswer.computeIfAbsent(r.getAnswerID(), k -> new ArrayList<>()).add(r);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        VBox reviewList = new VBox(10);
        for (Map.Entry<Integer, List<Review>> entry : reviewsByAnswer.entrySet()) {
            int answerID = entry.getKey();
            List<Review> reviews = entry.getValue();
            reviews.sort(Comparator.comparingInt(Review::getReviewID).reversed());

            // Show latest review for each answer
            Review latest = reviews.get(0);
            Label reviewLabel = new Label("Answer ID: " + answerID + " — " +
                    (latest.getRating() ? "Helpful" : "Not Helpful") + "\n" + latest.getDescription());
            reviewLabel.setWrapText(true);
            reviewLabel.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #ccc;");

            
            // Button to view review history
            Button historyButton = new Button("View History");
            historyButton.setOnAction(e -> {
                Stage historyStage = new Stage();
                VBox historyLayout = new VBox(10);
                historyLayout.setPadding(new Insets(15));

                for (Review r : reviews) {
                    Label rLabel = new Label("Review ID: " + r.getReviewID() + " — " +
                            (r.getRating() ? "Helpful" : "Not Helpful") + "\n" + r.getDescription());
                    rLabel.setWrapText(true);
                    rLabel.setStyle("-fx-background-color: #f9f9f9; -fx-padding: 8; -fx-border-color: #ddd;");
                    historyLayout.getChildren().add(rLabel);
                }

                Scene historyScene = new Scene(historyLayout, 400, 300);
                historyStage.setScene(historyScene);
                historyStage.setTitle("Review History for Answer " + answerID);
                historyStage.show();
            });
            HBox reviewLabelBox = new HBox(10);
            reviewLabelBox.getChildren().add(reviewLabel);
            boolean hasMessages = false;
            try {
                ArrayList<StudentReviewerMessage> messages = databaseHelper.getAllStudentReviewerMessages();
                for (StudentReviewerMessage message : messages) {
                    if (message.getReviewer().equals(currentUser.getUserName()) &&
                        message.getReviewID() == reviews.get(0).getReviewID()) {
                        hasMessages = true;
                        break;
                    }
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }

            if (hasMessages) {
                Button viewPMButton = new Button("View Messages");
                viewPMButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0077B5;");
                viewPMButton.setOnAction(a -> {
                    new StudentReviewerMessageUsersPage(databaseHelper, currentUser).show(primaryStage, reviews.get(0));
                });
                reviewLabelBox.getChildren().add(viewPMButton);
            }
            
            VBox reviewBox = new VBox(5, reviewLabelBox, historyButton);
            reviewList.getChildren().add(reviewBox);
        }

        layout.getChildren().add(reviewList);

        // Final scene setup
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Reviewer Profile");
        primaryStage.show();
    }
}
