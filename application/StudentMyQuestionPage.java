package application;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import databasePart1.DatabaseHelper;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * <p> Title: Student My Question Page </p>
 *
 * <p> Description: This class provides the user interface for displaying the full details of a question
 * posted by a student. It shows all answers associated with the question, highlights the resolving answer,
 * and allows the student to mark or unmark an answer as resolving. The page also supports navigation back
 * to the student's question list. </p>
 *
 * <p> Copyright: © 2025 </p>
 *
 *
 * @version 2.00        2025        Implementation of TP2 and TP3 question detail view
 */
public class StudentMyQuestionPage {

    private final DatabaseHelper databaseHelper;

    /**
     * <p> Method: StudentMyQuestionPage constructor </p>
     *
     * <p> Description: Initializes the StudentMyQuestionPage with a reference to the database helper. </p>
     *
     * @param databaseHelper The helper class for database operations.
     */
    public StudentMyQuestionPage(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * <p> Method: show </p>
     *
     * <p> Description: Displays the full detail view of a specific question posted by the student.
     * The page includes all answers, highlights the resolving answer, and allows the student to
     * toggle resolution status. </p>
     *
     * @param primaryStage The main application window.
     * @param currentUser The student currently logged in.
     * @param questionID The ID of the question to display.
     */
    public void show(Stage primaryStage, User currentUser, int questionID) {
    	
    	// Mark all answers as viewed when student opens the question
    	try {
    		databaseHelper.markAnswersViewed(questionID);
    	} catch (SQLException e) {
    		e.printStackTrace();
    	}
    	
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #f3f2ef;");

        // Back button to return to StudentMyQuestionsPage
        Button backButton = new Button("Back to My Questions");
        backButton.setOnAction(e -> {
            new StudentMyQuestionsPage(databaseHelper).show(primaryStage, currentUser);
        });

        // Get question details from database
        Question question;
        try {
            question = databaseHelper.getQuestionByID(questionID);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Display question title and description
        Label questionTitle = new Label("Question:");
        questionTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label questionText = new Label(question.getText());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-font-size: 14px;");

        // VBox for question section
        VBox questionBox = new VBox(10);
        questionBox.setStyle("-fx-background-color: white; -fx-padding: 15;");
        questionBox.getChildren().addAll(questionTitle, questionText);

        // Label for answers section
        Label answersHeader = new Label("Answers:");
        answersHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // VBox to contain answer rows
        VBox answersContainer = new VBox(10);

        // Get all answers for the question
        ArrayList<Answer> answerList;
        try {
            answerList = databaseHelper.getAllAnswers(questionID);
            // Sort resolved answers on top
            answerList.sort((a1, a2) -> Boolean.compare(!a1.getResolving(), !a2.getResolving()));
            
            for (Answer answer : answerList) {
                VBox answerBox = new VBox(5);
                answerBox.setStyle("-fx-background-color: white; -fx-padding: 10;");

                // Label for author
                Label authorLabel = new Label("Answered by: " + answer.getAuthor());
                authorLabel.setStyle("-fx-font-size: 12px;");

                // Label for answer text
                Label answerText = new Label(answer.getAnswerText());
                answerText.setWrapText(true);
                answerText.setStyle("-fx-font-size: 14px;");

                // Highlight resolving answer
                if (answer.getResolving()) {
                    answerBox.setStyle("-fx-background-color: #d0f0c0; -fx-padding: 10;");
                }

                // Logic to toggle resolving answer on click
                answerBox.setOnMouseClicked(event -> {
                    try {
                        if (answer.getResolving()) {
                            // If already resolved, clear it
                            databaseHelper.clearResolvingAnswer(questionID);
                            databaseHelper.markQuestionUnresolved(questionID);
                        } else {
                            // Clear any previous resolution
                            databaseHelper.clearResolvingAnswer(questionID);

                            // Mark this answer as resolving
                            databaseHelper.updateAnswerResolving(answer.getID(), true);
                            databaseHelper.markQuestionResolved(questionID);
                        }

                        // Refresh the page
                        show(primaryStage, currentUser, questionID);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });

                answerBox.getChildren().addAll(authorLabel, answerText);
                answersContainer.getChildren().add(answerBox);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        container.getChildren().addAll(backButton, questionBox, answersHeader, answersContainer);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        Scene scene = new Scene(scrollPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("My Question Details");
        primaryStage.show();
    }

}
