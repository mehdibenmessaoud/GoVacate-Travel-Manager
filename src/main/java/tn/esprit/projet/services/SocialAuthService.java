package tn.esprit.projet.services;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;
import com.restfb.types.User;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SocialAuthService {

    // Configure these with your actual client secrets and redirect URI
    private static final String GOOGLE_CLIENT_ID = "288971018401-412qdhg19c941tghpebau63vtognlt11.apps.googleusercontent.com";

    private static final String FACEBOOK_APP_ID = "1951053559123380";

    private static final String REDIRECT_URI = "https://localhost";

    public void authenticateWithGoogle(Consumer<Map<String, String>> onSuccess, Consumer<String> onError) {
        // Google OAuth implementation requires additional configuration
        // This is a placeholder that shows a dialog instead
        Platform.runLater(() -> {
            Stage authStage = new Stage();
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=" + GOOGLE_CLIENT_ID +
                    "&redirect_uri=" + REDIRECT_URI +
                    "&response_type=code" +
                    "&scope=openid%20email%20profile";

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    String location = webEngine.getLocation();
                    if (location.contains("code=")) {
                        String code = extractCode(location);
                        if (code != null) {
                            authStage.close();
                            Map<String, String> userInfo = new HashMap<>();
                            userInfo.put("id", "google_" + System.currentTimeMillis());
                            userInfo.put("email", "user@example.com");
                            Platform.runLater(() -> onSuccess.accept(userInfo));
                        }
                    } else if (location.contains("error=")) {
                        authStage.close();
                        onError.accept("Google Auth Error: Access denied");
                    }
                }
            });

            webEngine.load(googleAuthUrl);
            authStage.setScene(new Scene(webView, 600, 700));
            authStage.initModality(Modality.APPLICATION_MODAL);
            authStage.setTitle("Google Login");
            authStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo/logo.jpg")));
            authStage.show();
        });
    }

    public void authenticateWithFacebook(Consumer<Map<String, String>> onSuccess, Consumer<String> onError) {
        String loginUrl = "https://www.facebook.com/v12.0/dialog/oauth?" +
                "client_id=" + FACEBOOK_APP_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&scope=email,public_profile" +
                "&response_type=token";

        Platform.runLater(() -> {
            Stage authStage = new Stage();
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    String location = webEngine.getLocation();
                    if (location.startsWith(REDIRECT_URI)) {
                        String accessToken = extractAccessToken(location);
                        if (accessToken != null) {
                            authStage.close();
                            new Thread(() -> {
                                try {
                                    FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.LATEST);
                                    User user = facebookClient.fetchObject("me", User.class, com.restfb.Parameter.with("fields", "name,email,picture"));
                                    Map<String, String> userInfo = new HashMap<>();
                                    userInfo.put("id", user.getId());
                                    userInfo.put("name", user.getName());
                                    userInfo.put("email", user.getEmail());
                                    Platform.runLater(() -> onSuccess.accept(userInfo));
                                } catch (Exception e) {
                                    Platform.runLater(() -> onError.accept("Facebook Auth Error: " + e.getMessage()));
                                }
                            }).start();
                        } else if (location.contains("error=")) {
                            authStage.close();
                            onError.accept("Facebook Auth Error: Access denied");
                        }
                    }
                }
            });

            webEngine.load(loginUrl);
            authStage.setScene(new Scene(webView, 600, 700));
            authStage.initModality(Modality.APPLICATION_MODAL);
            authStage.setTitle("Facebook Login");
            authStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo/logo.jpg")));
            authStage.show();
        });
    }

    private String extractCode(String url) {
        if (url.contains("code=")) {
            String[] params = url.split("code=");
            if (params.length > 1) {
                return params[1].split("&")[0];
            }
        }
        return null;
    }

    private String extractAccessToken(String url) {
        if (url.contains("access_token=")) {
            String[] params = url.split("access_token=");
            if (params.length > 1) {
                return params[1].split("&")[0];
            }
        }
        return null;
    }
}
