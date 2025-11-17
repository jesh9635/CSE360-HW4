package application;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.SQLException;
import java.util.ArrayList;
import databasePart1.DatabaseHelper;

/**
 * The StudentReviewerMessagePage class displays the private message chain between students and reviewers.
 * 
 */
public class StudentReviewerMessagePage {
	
    private final DatabaseHelper databaseHelper;
    private final User currentUser;
    
    /**
     * Initializes the StudentReviewerMessagePage with the database helper and logged in user
     *
     * @param databaseHelper The helper class for interfacing with the database.
     * @param user The user who is currently logged in.
     */
    public StudentReviewerMessagePage(DatabaseHelper databaseHelper, User user) {
        this.databaseHelper = databaseHelper;
        this.currentUser = user;
    }

    private boolean userInList(String userName, ArrayList<User> userList) {
    	if (!userList.isEmpty()) {
        	for (User user : userList) {
        		if (user.getUserName().equals(userName)) {
        			return true;
        		}
        	}	
    	}
    	return false;
    }
    public void show(Stage primaryStage, Review currentReview, User otherUser) {
    	// VBox for containing all UI elements in page
        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #f3f2ef;");

        // Back button 
        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
        	new StudentHomePage(databaseHelper).show(primaryStage, currentUser);
        	//new StudentPrivateMessageUsersPage(databaseHelper, currentUser).show(primaryStage, currentReview);
        });
        
        // Label for a header
        Label header = new Label("Private Message Chain");
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        
        VBox addNewMessageBox = new VBox(10);
        addNewMessageBox.setPadding(new Insets(15));
        addNewMessageBox.setStyle("-fx-background-color: white");
        
        
        // Label for a header
        Label textBoxLabel = new Label("Post a response:");
        textBoxLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // TextArea object for input field
        TextArea newMessageText = new TextArea();
        newMessageText.setPromptText("Type your message here");
        newMessageText.setWrapText(true);
        newMessageText.setPrefHeight(100);
        
        // Button to submit new message
        Button submitMessageButton = new Button("Post");
        submitMessageButton.setOnAction(e -> {
            String messageText = newMessageText.getText();
            if (!messageText.isEmpty()) {
                StudentReviewerMessage p = new StudentReviewerMessage(messageText, currentUser.getUserName(), otherUser.getUserName(), currentReview.getReviewID(), 0, false);
                // Add message to database
                try {
					if (databaseHelper.createStudentReviewerMessage(p)) {
					    // Refresh the page to show the new question
					    show(primaryStage, currentReview, otherUser);
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
            }
        });
        addNewMessageBox.getChildren().addAll(textBoxLabel, newMessageText, submitMessageButton);

        // VBox for displaying existing reviewMessages
        VBox listContainer = new VBox(10);
        
        // Get all reviewerMessages and if the review that it concerns isn't in question list, add it.
        ArrayList<StudentReviewerMessage> databaseStudentReviewerMessages = new ArrayList<StudentReviewerMessage>();
        ArrayList<StudentReviewerMessage> studentReviewerMessageSubset = new ArrayList<StudentReviewerMessage>();
		try {
			databaseStudentReviewerMessages = databaseHelper.getAllStudentReviewerMessages();
			// For each question create a VBox
	        for (StudentReviewerMessage studentReviewerMessage : databaseStudentReviewerMessages) {
	        	if (studentReviewerMessage.getReviewID() == currentReview.getReviewID()) {
	        		//Check if sender and receiver are currentUser and otherUser or vice versa
	        		if ((studentReviewerMessage.getStudent().equals(currentUser.getUserName())
    				&& studentReviewerMessage.getReviewer().equals(otherUser.getUserName()))
    				||  (studentReviewerMessage.getReviewer().equals(currentUser.getUserName()) 
					&& studentReviewerMessage.getStudent().equals(otherUser.getUserName()))) {
	        			if (studentReviewerMessage.getReviewer().equals(currentUser.getUserName())){
	        				studentReviewerMessage.setSeenStatusReviewer(true);
	        				databaseHelper.updateStudentReviewerMessage(studentReviewerMessage);
	        			}
	        			studentReviewerMessageSubset.add(studentReviewerMessage);
	        		}
	        	}
	        }
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (studentReviewerMessageSubset.isEmpty()) {
			Label emptyStudentReviewerMessage = new Label("You have no messages");
			listContainer.getChildren().add(emptyStudentReviewerMessage);
		} else {
			for (StudentReviewerMessage studentReviewerMessage : studentReviewerMessageSubset) {
		        VBox studentReviewerMessageBox = createStudentReviewerMessageBox(studentReviewerMessage, primaryStage);
		        listContainer.getChildren().add(studentReviewerMessageBox);
			}		
		}


        // ScrollPane to allow scrolling through questions
        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);

        container.getChildren().addAll(backButton, header, scrollPane, addNewMessageBox);
        
        // Set up the scene and show
        Scene scene = new Scene(container, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Private Message Conversation Page");
        primaryStage.show();
    }

    
    /**
     * Creates a VBox for a single message.
     *
     * @param studentReviewerMessage The message to display.
     * @param primaryStage The parent stage for this application.
     * @return A VBox containing the question.
     */
    private VBox createStudentReviewerMessageBox(StudentReviewerMessage studentReviewerMessage, Stage primaryStage) {
    	// VBox to contain all UI elements
        VBox studentReviewerMessageBox = new VBox(5);
        studentReviewerMessageBox.setStyle(
            "-fx-background-color: white; " +
            "-fx-padding: 10;"
        );
        
        // Label for author field
        Label authorLabel = new Label(studentReviewerMessage.getStudent()+": ");
        authorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        
        // Label to display studentReviewerMessage text
        Label messageText = new Label(studentReviewerMessage.getMessage());
        messageText.setStyle("-fx-font-size: 14px;");
        messageText.setWrapText(true);
        messageText.prefWidthProperty().bind(primaryStage.widthProperty().subtract(60));


        studentReviewerMessageBox.getChildren().addAll(authorLabel, messageText);
        return studentReviewerMessageBox;
    }
}
