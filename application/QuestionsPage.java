package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.ArrayList;
import databasePart1.DatabaseHelper;

/**
 * <p> Title: Questions Page </p>
 *
 * <p> Description: This class provides the main Q&A interface for students and reviewers.
 * It allows users to post new questions, view existing questions, search by keyword, and interact
 * with answers and reviews. Students can edit or delete their own questions, send private messages,
 * and view reviews. Reviewers can write or update reviews for answers. The layout adapts based on
 * the user's role. </p>
 *
 * <p> Copyright: © 2025 </p>
 *
 *
 * @version 2.00        2025        Implementation of TP2 and TP3 Q&A interface
 */
public class QuestionsPage {

    private final DatabaseHelper databaseHelper;
    private final User currentUser;
    private final String currentRole;

    /**
     * <p> Constructor: QuestionsPage </p>
     *
     * <p> Description: Initializes the QuestionsPage with a database helper, the current user,
     * and their active role. </p>
     *
     * @param databaseHelper The helper class for interfacing with the database.
     * @param user The user who is currently logged in.
     * @param currentRole The active role the user is operating under.
     */
    public QuestionsPage(DatabaseHelper databaseHelper, User user, String currentRole) {
        this.databaseHelper = databaseHelper;
        this.currentUser = user;
        this.currentRole = currentRole.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * <p> Method: show </p>
     *
     * <p> Description: Displays the main Q&A interface. Includes a search bar, question submission form,
     * and a scrollable list of existing questions. Each question includes action buttons based on the
     * user's role and authorship. </p>
     *
     * @param primaryStage The main application window.
     */
    public void show(Stage primaryStage) {

        // VBox for containing all UI elements in page
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #f3f2ef;");

        // Back button to return to the appropriate home page
        Button backButton = new Button("Back to Home");
        backButton.setOnAction(e -> {
            if (currentRole.equals("reviewer")) {
                new ReviewerProfilePage(databaseHelper, currentUser).show(primaryStage, currentUser.getUserName(), null);
            } else if (currentRole.equals("staff")) {
                new StaffHomePage(databaseHelper).show(primaryStage, currentUser);
            } else if (currentRole.equals("student")) {
                new StudentHomePage(databaseHelper).show(primaryStage, currentUser);
            } else {
                new RoleSelectionPage(databaseHelper).show(primaryStage, currentUser);
            }
        });

        // HBox for the search bar and button
        HBox searchBox = new HBox(10);
        searchBox.setPadding(new Insets(0, 0, 10, 0));
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search questions...");
        searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 8px; -fx-border-color: #ccc; -fx-border-radius: 8px; -fx-padding: 7px;");
        HBox.setHgrow(searchField, javafx.scene.layout.Priority.ALWAYS); // Resize search bar

        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #0077B5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 8px 15px;");
        searchBox.getChildren().addAll(searchField, searchButton);

        // VBox for containing new question section
        VBox addQuestionBox = new VBox(10);
        addQuestionBox.setPadding(new Insets(15));
        addQuestionBox.setStyle("-fx-background-color: white");

        // Label for a header
        Label askQuestionLabel = new Label("Ask a new question:");
        askQuestionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // TextArea object for input field
        TextArea newQuestionText = new TextArea();
        newQuestionText.setPromptText("Type your question here");
        newQuestionText.setWrapText(true);
        newQuestionText.setPrefHeight(100);

        // Button to submit new question
        Button submitQuestionButton = new Button("Submit Question");
        submitQuestionButton.setStyle("-fx-background-color: #0077B5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 8px 15px;");
        submitQuestionButton.setOnAction(e -> {
            String questionText = newQuestionText.getText();
            if (!questionText.isEmpty()) {
                Question question = new Question(currentUser.getUserName(), questionText, false, 0);
                try {
                    if (databaseHelper.createQuestion(question)) {
                        show(primaryStage);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Duplicate Question");
                        alert.setHeaderText("This question already exists.");
                        alert.setContentText("Please ask a different question.");
                        alert.showAndWait();
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        addQuestionBox.getChildren().addAll(askQuestionLabel, newQuestionText, submitQuestionButton);

        // VBox for displaying existing questions
        VBox listContainer = new VBox(10);

        // Get all questions from database and store in array list
        ArrayList<Question> questionsList;
        try {
            questionsList = databaseHelper.getAllQuestions();

            // Sort resolved first then unresolved
            questionsList.sort((q1, q2) -> {
                if (q1.getResolved() != q2.getResolved()) {
                    return Boolean.compare(q2.getResolved(), q1.getResolved());
                }
                return Integer.compare(q2.getQuestionID(), q1.getQuestionID()); // newest first
            });

            // For each question create a VBox
            for (Question question : questionsList) {
                VBox questionBox = createQuestionBox(question, primaryStage);
                listContainer.getChildren().add(questionBox);
            }

            // If search button clicked display only questions that match keyword
            searchButton.setOnAction(e -> {
                String searchTerm = searchField.getText().toLowerCase();
                listContainer.getChildren().clear(); // Clear the existing list

                for (Question question : questionsList) {
                    if (question.getText().toLowerCase().contains(searchTerm)) {
                        VBox questionBox = createQuestionBox(question, primaryStage);
                        listContainer.getChildren().add(questionBox);
                    }
                }
            });

        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        // ScrollPane to allow scrolling through questions
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);

        container.getChildren().addAll(backButton, addQuestionBox, searchBox, scrollPane);

        // Set up the scene and show
        Scene scene = new Scene(container, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Q&A Home");
        primaryStage.show();
    }

    /**
     * <p> Method: createQuestionBox </p>
     *
     * <p> Description: Creates a visual container for a single question. Includes metadata, answer/review
     * navigation, and role-specific actions such as editing, deleting, reviewing, and messaging. </p>
     *
     * @param question The question to display.
     * @param primaryStage The parent stage for this application.
     * @return A VBox containing the question and its associated controls.
     */
    private VBox createQuestionBox(Question question, Stage primaryStage) {
        VBox questionBox = new VBox(5);
        questionBox.setStyle("-fx-background-color: white; -fx-padding: 10;");

        HBox authorLine = new HBox(5);
        authorLine.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label("Asked by: " + question.getAuthor());
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        authorLine.getChildren().add(authorLabel);

        if (question.getResolved()) {
            Label resolvedLabel = new Label("[Resolved]");
            resolvedLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
            authorLine.getChildren().add(resolvedLabel);
        }

        Label questionText = new Label(question.getText());
        questionText.setStyle("-fx-font-size: 14px;");
        questionText.setWrapText(true);
        questionText.prefWidthProperty().bind(primaryStage.widthProperty().subtract(60));

        HBox buttonBox = new HBox(10);

        // Button to view answers for the question
        Button viewAnswersButton = new Button("View Answers");
        viewAnswersButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0077B5;");
        viewAnswersButton.setOnAction(e -> new AnswerPage(databaseHelper, currentUser).show(primaryStage, question));
        buttonBox.getChildren().add(viewAnswersButton);

        // View Reviews Button
        Button viewReviewsButton = new Button("View Reviews");
        viewReviewsButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0077B5;");
        viewReviewsButton.setOnAction(e -> {
            new AnswerReviewsPage(databaseHelper, currentUser).show(primaryStage, question);
        });
        buttonBox.getChildren().add(viewReviewsButton);

        // REVIEW BUTTON FOR REVIEWERS
        if (currentRole.contains("reviewer")) {
            Button reviewButton = new Button("Write/Update Review");
            reviewButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0077B5;");
            reviewButton.setOnAction(e -> {
                System.out.println("Review button clicked");
                try {
                    ArrayList<Answer> answers = databaseHelper.getAllAnswers(question.getQuestionID());
                    if (answers.isEmpty()) return;

                    // Select resolving answer if available
                    // Use resolving answer if available, otherwise default to first
                    Answer targetAnswer = answers.stream().filter(Answer::getResolving).findFirst().orElse(answers.get(0));

                    // Get existing review if one exists
                    ArrayList<Review> existingReviews = databaseHelper.getAllUserReviews(currentUser.getUserName());
                    final Review[] existing = new Review[1];
                    for (Review r : existingReviews) {
                        if (r.getAnswerID() == targetAnswer.getID()) {
                            existing[0] = r;
                            break;
                        }
                    }

                    // Create review popup window
                    Stage reviewStage = new Stage();
                    VBox reviewLayout = new VBox(10);
                    reviewLayout.setPadding(new Insets(15));

                    // Show context of the answer being reviewed
                    Label contextLabel = new Label("Reviewing Answer: \"" + targetAnswer.getAnswerText() + "\"");
                    contextLabel.setWrapText(true);
                    contextLabel.setStyle("-fx-font-size: 13px; -fx-font-style: italic;");

                    Label header = new Label(existing[0] == null ? "Write a Review" : "Update Your Review");
                    header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

                    TextArea descriptionArea = new TextArea();
                    descriptionArea.setWrapText(true);
                    descriptionArea.setPromptText("Describe why this answer was helpful or not...");
                    if (existing[0] != null) descriptionArea.setText(existing[0].getDescription());

                    ToggleGroup ratingGroup = new ToggleGroup();
                    RadioButton helpful = new RadioButton("Helpful");
                    RadioButton notHelpful = new RadioButton("Not Helpful");
                    helpful.setToggleGroup(ratingGroup);
                    notHelpful.setToggleGroup(ratingGroup);
                    if (existing[0] != null) {
                        if (existing[0].getRating()) helpful.setSelected(true);
                        else notHelpful.setSelected(true);
                    }

                    // Button to submit review
                    Button submitReview = new Button("Submit");
                    submitReview.setOnAction(a -> {
                        boolean rating = helpful.isSelected();
                        String desc = descriptionArea.getText();
                        if (!desc.isEmpty() && ratingGroup.getSelectedToggle() != null) {
                            try {
                                // Always insert new version (even if updating)
                                databaseHelper.updateReview(currentUser.getUserName(), targetAnswer.getID(), rating, desc);
                                reviewStage.close();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    reviewLayout.getChildren().addAll(contextLabel, header, helpful, notHelpful, descriptionArea, submitReview);
                    Scene reviewScene = new Scene(reviewLayout);
                    reviewStage.setScene(reviewScene);
                    reviewStage.setTitle("Review Answer");
                    reviewStage.show();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            buttonBox.getChildren().add(reviewButton);
        }

        // If the current user is the author of the question, show edit and delete buttons
        if (currentUser.getUserName().equals(question.getAuthor())) {
            Button editButton = new Button("Edit");
            editButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #555;");
            editButton.setOnAction(e -> {
                Stage editStage = new Stage();
                VBox editLayout = new VBox(10);
                editLayout.setPadding(new Insets(15));

                TextArea editTextArea = new TextArea(question.getText());
                editTextArea.setWrapText(true);
                editTextArea.setPrefSize(400, 150);

                Button saveButton = new Button("Save");
                saveButton.setOnAction(saveEvent -> {
                    String newText = editTextArea.getText();
                    if (!newText.isEmpty()) {
                        try {
                            databaseHelper.updateQuestion(question.getQuestionID(), newText);
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                        show(primaryStage);
                        editStage.close();
                    }
                });

                editLayout.getChildren().addAll(editTextArea, saveButton);
                Scene editScene = new Scene(editLayout);
                editStage.setScene(editScene);
                editStage.setTitle("Edit Question");
                editStage.show();
            });

            Button deleteButton = new Button("Delete");
            deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #D32F2F;");
            deleteButton.setOnAction(a -> {
                try {
                    databaseHelper.deleteQuestion(question.getQuestionID());
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                show(primaryStage);
            });

            boolean hasPrivateMessages = false;
            try {
                ArrayList<PrivateMessage> privateMessages = databaseHelper.getAllPrivateMessages();
                for (PrivateMessage privateMessage : privateMessages) {
                    if (privateMessage.getReceiver().equals(currentUser.getUserName()) &&
                        privateMessage.getQuestionID() == question.getQuestionID()) {
                        hasPrivateMessages = true;
                        break;
                    }
                }
            } catch (SQLException e1) {
                e1.printStackTrace();
            }

            if (hasPrivateMessages) {
                Button viewPMButton = new Button("View Private Messages");
                viewPMButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0077B5;");
                viewPMButton.setOnAction(a -> {
                    new StudentPrivateMessageUsersPage(databaseHelper, currentUser).show(primaryStage, question);
                });
                buttonBox.getChildren().add(viewPMButton);
            }

            buttonBox.getChildren().addAll(editButton, deleteButton);
        }

        // If the current user is not the author of the question, show send private message button
        if (!currentUser.getUserName().equals(question.getAuthor())) {
            Button viewPMButton = new Button("Send Message");
            viewPMButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #0077B5;");
            viewPMButton.setOnAction(a -> {
                try {
                    ArrayList<User> users = databaseHelper.getAllUsers();
                    User otherUser = null;
                    for (User user : users) {
                        if (user.getUserName().equals(question.getAuthor())) {
                            otherUser = user;
                            break;
                        }
                    }
                    if (otherUser != null) {
                        new StudentPrivateMessagePage(databaseHelper, currentUser).show(primaryStage, question, otherUser);
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            });
            buttonBox.getChildren().add(viewPMButton);
        }

        questionBox.getChildren().addAll(authorLine, questionText, buttonBox);
        return questionBox;
    }
}
