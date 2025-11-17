package application;

import databasePart1.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;


/**
 * ElevatedMessageUsersPage handles allowing a staff member to interact with another 
 * staff member.
 * 
*/

public class ElevatedMessageUsersPage {
	
	private final DatabaseHelper databaseHelper;
	private String label;
	private boolean hasError;
	
	public ElevatedMessageUsersPage(DatabaseHelper databaseHelper) {
		this.databaseHelper = databaseHelper;
		label = "";
		hasError = false;
	}
	
	public ElevatedMessageUsersPage(DatabaseHelper databaseHelper, String label, boolean hasError) {
        this.databaseHelper = databaseHelper;
        this.label = label;
        this.hasError = hasError;
    }
	
	public void show(Stage primaryStage, User user) {
		// Label for title
		Label titleLabel = new Label("Choose Private Messages to View");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        
		// Input fields for userName, password, and invitation code
		TextField userNameField = new TextField();
        userNameField.setPromptText("Enter userName");
        String fieldStyle = "-fx-font-size: 14px; -fx-background-radius: 8px; -fx-border-color: #ccc; -fx-border-radius: 8px; -fx-padding: 10px;";
        userNameField.setStyle(fieldStyle);
        
        ChoiceBox<String> userFieldOne = new ChoiceBox<>();
        ArrayList<User> userList = new ArrayList<User>();
        try {
        	userList = databaseHelper.getAllUsers();
        } catch (SQLException e){
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
        for (User currUser : userList) {
        	if (!currUser.getUserName().equals(user.getUserName())) {
            	String[] currUserRole = currUser.getRole().split(",");
            	for (int i = 0; i < currUserRole.length; i++) {
                	if (currUserRole[i].equals("staff")) {
                    	userFieldOne.getItems().add(currUser.getUserName());	
                    	break;
                	}
            	}
        	}
        }
        userFieldOne.setValue("Choose User 1");
        userFieldOne.setMaxWidth(Double.MAX_VALUE);
        userFieldOne.setStyle("-fx-font-size: 14px; -fx-background-radius: 8px; -fx-border-color: #ccc; -fx-border-radius: 8px;");
        // Label to display error messages for invalid input or search issues, or success messages
        Label errorLabel = new Label();
        if(hasError) {
        	errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        	errorLabel.setText(label);
        }
        else {
        	errorLabel.setStyle("-fx-text-fill: blue; -fx-font-size: 12px;");
        	errorLabel.setText(label);
        }
        
        // Button to add role
        Button viewMessages = new Button("View Message");
        viewMessages.setMaxWidth(Double.MAX_VALUE);
        viewMessages.setStyle(
            "-fx-background-color: #0077B5; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 12px;"
        );
        
        
        // Button to direct back to AdminHomePage
        Button backButton = new Button("Back to Home Page");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setStyle(
            "-fx-background-color: #E0E0E0; -fx-text-fill: #333; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 12px;"
        );
        
        viewMessages.setOnAction(a -> {
    		try {
            	ArrayList<User> list = databaseHelper.getAllUsers();
        		User userOne = null;
        		boolean userOneInit = false;
        		for (User cUser : list) {
        			if (cUser.getUserName().equals(userFieldOne.getValue())) {
        				userOne = cUser;
        				userOneInit = true;
        				break;
        			}
        		}
           		if (userOneInit) {
            		new AllMessagesConvoPage(databaseHelper, user).show(primaryStage, userOne, true);
           		}
    		} catch(SQLException e) {
                System.err.println("Database error: " + e.getMessage());
                e.printStackTrace();
    		}
        });
        
        
        backButton.setOnAction(a->{
        	new AdminHomePage(databaseHelper).show(primaryStage, user);
        });
        
        // VBox for the white border containing the content
        VBox contentCard = new VBox(15);
        contentCard.setAlignment(Pos.CENTER);
        contentCard.setPadding(new Insets(40, 40, 40, 40));
        contentCard.setMaxWidth(400);
        contentCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 10px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);"
        );
        contentCard.getChildren().addAll(titleLabel, userFieldOne, errorLabel, viewMessages,  backButton);
        
        // StackPane for grey background
        StackPane rootLayout = new StackPane(contentCard);
        rootLayout.setStyle("-fx-background-color: #f3f2ef;");
        rootLayout.setPadding(new Insets(20));
        
        // Set scene and primaryStage
        primaryStage.setScene(new Scene(rootLayout, 800, 600));
        primaryStage.setTitle("View Any Messages");
        primaryStage.show();
	}
}

