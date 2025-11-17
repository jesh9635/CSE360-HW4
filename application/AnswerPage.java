package application;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.ArrayList;
import databasePart1.DatabaseHelper;

/**
 * <p> Title: Answer Page </p>
 *
 * <p> Description: This class provides the user interface for viewing and managing answers to a specific question.
 * Users can view all answers, post new ones, and—if they are the author—edit or delete their own answers.
 * The page also displays trust-weighted ratings for each answer and highlights resolving answers.
 * Navigation adapts based on the user's role. </p>
 *
 * <p> Copyright: © 2025 </p>
 *
 *
 * @version 2.00        2025        Implementation of TP2 and TP3 answer interaction interface
 */
public class AnswerPage {

    private final DatabaseHelper databaseHelper;
    private final User currentUser;

    /**
     * <p> Constructor: AnswerPage </p>
     *
     * <p> Description: Initializes the AnswerPage with a reference to the database helper and the current user. </p>
     *
     * @param databaseHelper The helper class for database operations.
     * @param user The user who is currently logged in.
     */
    public AnswerPage(DatabaseHelper databaseHelper, User user) {
        this.databaseHelper = databaseHelper;
        this.currentUser = user;
    }

    /**
     * <p> Method: show </p>
     *
     * <p> Description: Displays the answer page for a given question. Includes the question details,
     * a list of answers with trust-weighted ratings, and controls for posting, editing, or deleting answers.
     * Resolving answers are highlighted, and navigation adapts based on user role. </p>
     *
     * @param primaryStage The primary stage for this application.
     * @param question The question for which to display the answers.
     */
    public void show(Stage primaryStage, Question question) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #f3f2ef;");

        Label questionHeader = new Label("Question:");
        questionHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox questionBox = new VBox(5);
        questionBox.setStyle("-fx-background-color: white; -fx-padding: 15;");

        Label questionAuthorLabel = new Label("Asked by: " + question.getAuthor());
        questionAuthorLabel.setStyle("-fx-font-size: 12px");

        Label questionTextLabel = new Label(question.getText());
        questionTextLabel.setStyle("-fx-font-size: 14px;");
        questionTextLabel.setWrapText(true);

        questionBox.getChildren().addAll(questionAuthorLabel, questionTextLabel);

        VBox addAnswerBox = new VBox(10);
        addAnswerBox.setPadding(new Insets(15));
        addAnswerBox.setStyle("-fx-background-color: white;");

        Label addAnswerHeader = new Label("Post an Answer:");
        addAnswerHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea newAnswerText = new TextArea();
        newAnswerText.setPromptText("Type here...");
        newAnswerText.setWrapText(true);

        Button submitAnswerButton = new Button("Submit Answer");
        submitAnswerButton.setStyle("-fx-background-color: #0077B5; -fx-text-fill: white; -fx-font-weight: bold;");
        submitAnswerButton.setOnAction(e -> {
            String answerText = newAnswerText.getText();
            if (!answerText.isEmpty()) {
                Answer newAnswer = new Answer(currentUser.getUserName(), answerText, false, false, 0, question.getQuestionID());
                try {
                    if (databaseHelper.doesAnswerExist(newAnswer)) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Duplicate Answer");
                        alert.setHeaderText("This answer already exists.");
                        alert.setContentText("Please provide a different answer.");
                        alert.showAndWait();
                    } else {
                        databaseHelper.createAnswer(newAnswer);
                        show(primaryStage, question);
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        addAnswerBox.getChildren().addAll(addAnswerHeader, newAnswerText, submitAnswerButton);

        Label answersHeader = new Label("Answers:");
        answersHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox answerListContainer = new VBox(10);
        ArrayList<Answer> answerList;
        try {
            answerList = databaseHelper.getAllAnswers(question.getQuestionID());
            for (Answer answer : answerList) {
                VBox answerBox = new VBox(5);
                answerBox.setStyle("-fx-background-color: white; -fx-padding: 10;");
                if (answer.getResolving()) {
                    answerBox.setStyle("-fx-background-color: #d0f0c0; -fx-padding: 10;");
                }

                Label authorLabel = new Label("Answered by: " + answer.getAuthor());
                authorLabel.setStyle("-fx-font-size: 12px;");

                Label answerLabel = new Label(answer.getAnswerText());
                answerLabel.setStyle("-fx-font-size: 14px;");
                answerLabel.setWrapText(true);
                
                try {
                    double rating = databaseHelper.calculateAnswerRating(answer.getID(), currentUser.getUserName());
                    Label ratingLabel = new Label("Rating: " + String.format("%.2f", rating));
                    ratingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #0077B5;");
                    answerBox.getChildren().add(ratingLabel);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                HBox answerButtonBox = new HBox(10);

                if (currentUser.getUserName().equals(answer.getAuthor())) {
                    Button editAnswerButton = new Button("Edit");
                    editAnswerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #555;");
                    editAnswerButton.setOnAction(e -> {
                        Stage editStage = new Stage();
                        VBox editLayout = new VBox(10);
                        editLayout.setPadding(new Insets(15));

                        TextArea editTextArea = new TextArea(answer.getAnswerText());
                        editTextArea.setWrapText(true);
                        editTextArea.setPrefSize(400, 150);

                        Button updateButton = new Button("Update");
                        updateButton.setOnAction(a -> {
                            String newText = editTextArea.getText();
                            if (!newText.isEmpty()) {
                                try {
                                    databaseHelper.updateAnswer(answer.getID(), newText);
                                } catch (SQLException e1) {
                                    e1.printStackTrace();
                                }
                                show(primaryStage, question);
                                editStage.close();
                            }
                        });

                        editLayout.getChildren().addAll(new Label("Edit your answer:"), editTextArea, updateButton);
                        Scene editScene = new Scene(editLayout);
                        editStage.setScene(editScene);
                        editStage.setTitle("Edit Answer");
                        editStage.show();
                    });

                    Button deleteAnswerButton = new Button("Delete");
                    deleteAnswerButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #D32F2F;");
                    deleteAnswerButton.setOnAction(e -> {
                        try {
                            databaseHelper.deleteAnswer(answer.getID());
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                        show(primaryStage, question);
                    });

                    answerButtonBox.getChildren().addAll(editAnswerButton, deleteAnswerButton);
                }

                answerBox.getChildren().addAll(authorLabel, answerLabel, answerButtonBox);
                answerListContainer.getChildren().add(answerBox);
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

        // Back button with role "aware" navigation
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            String normalizedRole = currentUser.getRole().toLowerCase().replaceAll("\\s+", "");
            if (normalizedRole.contains("reviewer") && !normalizedRole.contains("student")) {
                new ReviewerProfilePage(databaseHelper, currentUser).show(primaryStage, currentUser.getUserName(), null);
            } else {
                new QuestionsPage(databaseHelper, currentUser, "student").show(primaryStage);
            }
        });

        container.getChildren().addAll(backButton, questionHeader, questionBox, answersHeader, answerListContainer, addAnswerBox);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        Scene scene = new Scene(scrollPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("View Answers");
        primaryStage.show();
    }
}
