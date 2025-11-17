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
 * <p> Title: Answer Reviews Page </p>
 *
 * <p> Description: This class provides the interface for viewing reviews of the top answer to a given question.
 * Students and reviewers can filter reviews based on trust relationships, message reviewers privately,
 * view reviewer profiles, and update their own reviews. The layout adapts based on user role and review ownership. </p>
 *
 * <p> Copyright: © 2025 </p>
 *
 *
 * @version 1.00        2025        Initial implementation of TP3 answer review interface
 */
public class AnswerReviewsPage {

    private final DatabaseHelper databaseHelper;
    private final User currentUser;

    /**
     * Initializes the page with the database helper and current user.
     *
     * @param databaseHelper The helper class for database access.
     * @param currentUser The user currently logged in.
     */
    public AnswerReviewsPage(DatabaseHelper databaseHelper, User currentUser) {
        this.databaseHelper = databaseHelper;
        this.currentUser = currentUser;
    }

    /**
     * Displays the review page for the top answer to a given question.
     *
     * @param primaryStage The main application window.
     * @param question The question whose top answer is being reviewed.
     */
    public void show(Stage primaryStage, Question question) {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Header label
        Label header = new Label("Reviews for Top Answer");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Toggle to filter reviews by trusted reviewers
        CheckBox trustedOnlyToggle = new CheckBox("Show only trusted reviewers");

        VBox reviewList = new VBox(10);

        // Reload reviews when toggle is changed
        trustedOnlyToggle.setOnAction(e -> {
            try {
                displayReviews(reviewList, question, trustedOnlyToggle.isSelected(), primaryStage);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        // Initial review load (is un-filtered)
        try {
            displayReviews(reviewList, question, false, primaryStage);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Back button with role-aware navigation
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            String normalizedRole = currentUser.getRole().toLowerCase().replaceAll("\\s+", "");
            if (normalizedRole.contains("reviewer") && !normalizedRole.contains("student")) {
                new QuestionsPage(databaseHelper, currentUser, "reviewer").show(primaryStage);
            } else {
                new QuestionsPage(databaseHelper, currentUser, "student").show(primaryStage);
            }
        });

        layout.getChildren().addAll(backButton, header, trustedOnlyToggle, reviewList);

        // Final scene setup
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Answer Reviews");
        primaryStage.show();
    }

    /**
     * Loads and displays reviews for the top answer to the given question.
     *
     * @param container The VBox container to populate with review boxes.
     * @param question The question whose answer is being reviewed.
     * @param trustedOnly Whether to filter reviews by trusted reviewers.
     * @param primaryStage The main application window.
     * @throws SQLException If database access fails.
     */
    private void displayReviews(VBox container, Question question, boolean trustedOnly, Stage primaryStage) throws SQLException {
        container.getChildren().clear();

        ArrayList<Answer> answers = databaseHelper.getAllAnswers(question.getQuestionID());
        if (answers.isEmpty()) {
            container.getChildren().add(new Label("No answers available."));
            return;
        }

        // Use resolving answer if available, otherwise default to first
        Answer targetAnswer = answers.stream().filter(Answer::getResolving).findFirst().orElse(answers.get(0));

        ArrayList<Review> reviews = databaseHelper.getReviewsForAnswer(targetAnswer.getID(), currentUser.getUserName(), trustedOnly);

        if (reviews.isEmpty()) {
            container.getChildren().add(new Label("No reviews found."));
            return;
        }

        // Show each review
        for (Review review : reviews) {
            VBox reviewBox = new VBox(5);
            reviewBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-border-color: #ccc;");

            Label reviewer = new Label("Reviewer: " + review.getReviewerUsername());
            reviewer.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

            Label rating = new Label("Rating: " + (review.getRating() ? "Helpful" : "Not Helpful"));
            rating.setStyle("-fx-font-size: 13px;");

            Label description = new Label(review.getDescription());
            description.setWrapText(true);
            description.setStyle("-fx-font-size: 14px;");

            // Show update button if current user is the reviewer
            if (currentUser.getUserName().equals(review.getReviewerUsername())) {
                Button updateReviewButton = new Button("Update Review");
                updateReviewButton.setOnAction(e -> {
                    showReviewPopup(primaryStage, review.getAnswerID(), review.getDescription(), review.getRating());
                });
                reviewBox.getChildren().add(updateReviewButton);
            }

            // Button to message reviewer
            Button messageButton = new Button("Message Reviewer");
            messageButton.setOnAction(e -> {
                try {
                    ArrayList<User> users = databaseHelper.getAllUsers();
                    for (User u : users) {
                        if (u.getUserName().equals(review.getReviewerUsername())) {
                            new StudentReviewerMessagePage(databaseHelper, currentUser).show(primaryStage, review, u);
                            break;
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            // Button to view reviewer profile
            Button profileButton = new Button("View Reviewer Profile");
            profileButton.setOnAction(e -> {
                new ReviewerProfilePage(databaseHelper, currentUser).show(primaryStage, review.getReviewerUsername(), question);
            });

            reviewBox.getChildren().addAll(reviewer, rating, description, messageButton, profileButton);
            container.getChildren().add(reviewBox);
        }
    }

    /**
     * Displays a pop-up for the reviewer to update their review.
     *
     * @param primaryStage The main application window.
     * @param answerID The ID of the answer being reviewed.
     * @param existingDescription The current review description.
     * @param existingRating The current review rating.
     */
    private void showReviewPopup(Stage primaryStage, int answerID, String existingDescription, boolean existingRating) {
        Stage reviewStage = new Stage();
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label header = new Label("Update Your Review");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea descriptionArea = new TextArea(existingDescription);
        descriptionArea.setWrapText(true);

        // Rating toggle
        ToggleGroup ratingGroup = new ToggleGroup();
        RadioButton helpful = new RadioButton("Helpful");
        RadioButton notHelpful = new RadioButton("Not Helpful");
        helpful.setToggleGroup(ratingGroup);
        notHelpful.setToggleGroup(ratingGroup);
        if (existingRating) helpful.setSelected(true);
        else notHelpful.setSelected(true);

        // Submit updated review
        Button submit = new Button("Submit");
        submit.setOnAction(e -> {
            boolean rating = helpful.isSelected();
            String desc = descriptionArea.getText();
            if (!desc.isEmpty() && ratingGroup.getSelectedToggle() != null) {
                try {
                    databaseHelper.updateReview(currentUser.getUserName(), answerID, rating, desc);
                    reviewStage.close();
                    show(primaryStage, databaseHelper.getQuestionByID(answerID)); // Refresh page
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        layout.getChildren().addAll(header, helpful, notHelpful, descriptionArea, submit);
        reviewStage.setScene(new Scene(layout));
        reviewStage.setTitle("Review Answer");
        reviewStage.show();
    }
}
