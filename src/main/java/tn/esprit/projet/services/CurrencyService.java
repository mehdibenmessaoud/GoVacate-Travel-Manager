package tn.esprit.projet.services;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CurrencyService {
    // API gratuite (pas besoin de clé pour les taux de base, ou utilise une clé gratuite)
    private static final String API_URL = "https://open.er-api.com/v6/latest/TND";

    public static double getExchangeRate(String targetCurrency) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();

            JSONObject json = new JSONObject(result.toString());
            // On récupère le taux pour la devise cible (ex: EUR, USD)
            return json.getJSONObject("rates").getDouble(targetCurrency);

        } catch (Exception e) {
            System.out.println("Erreur conversion devise : " + e.getMessage());
            return 0.0;
        }
    }
}