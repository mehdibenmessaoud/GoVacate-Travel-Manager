package tn.esprit.projet.entities;
import java.util.List;

// This holds the IDs of the menus and the "Why" text from the AI
public record AIRecommendation(List<Integer> ids, String explanation) {}