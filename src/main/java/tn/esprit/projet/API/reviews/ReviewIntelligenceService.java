package tn.esprit.projet.API.reviews;

import tn.esprit.projet.API.common.ApiException;

import java.util.Set;

public class ReviewIntelligenceService {

    public static final String MODERATION_PREFIX = "[Avis masque par moderation]";

    private final SightengineApiClient sightengineApiClient;
    private final AzureLanguageSentimentApiClient azureLanguageSentimentApiClient;
    private static final Set<String> POSITIVE_TERMS = Set.of(
            "excellent", "parfait", "super", "genial", "magnifique", "top",
            "good", "great", "amazing", "clean", "comfortable", "recommande", "recommend"
    );
    private static final Set<String> NEGATIVE_TERMS = Set.of(
            "mauvais", "horrible", "nul", "sale", "bruyant", "cher", "decu",
            "bad", "awful", "terrible", "dirty", "noise", "noisy", "worst", "slow"
    );

    public ReviewIntelligenceService() {
        this.sightengineApiClient = new SightengineApiClient();
        this.azureLanguageSentimentApiClient = new AzureLanguageSentimentApiClient();
    }

    public ReviewProcessingResult processReview(String comment) {
        String rawComment = comment == null ? "" : comment.trim();
        if (rawComment.isEmpty()) {
            return new ReviewProcessingResult("", false, "Empty review", false, 0.0, 0.0, false);
        }

        SightengineApiClient.ModerationResult moderationResult =
                new SightengineApiClient.ModerationResult(false, 0.0, "Moderation not checked", false);
        try {
            moderationResult = sightengineApiClient.moderateText(rawComment);
        } catch (ApiException e) {
            moderationResult = new SightengineApiClient.ModerationResult(false, 0.0,
                    "Moderation skipped: " + e.getMessage(), false);
        }

        LocalSentiment sentimentResult = analyzeSentiment(rawComment);

        String finalComment = rawComment;
        boolean moderated = moderationResult.blocked();
        if (moderated) {
            String reason = moderationResult.reason() == null ? "" : moderationResult.reason().trim();
            finalComment = reason.isEmpty()
                    ? MODERATION_PREFIX
                    : MODERATION_PREFIX + " Motif: " + reason;
        }

        return new ReviewProcessingResult(
                finalComment,
                moderated,
                moderationResult.reason(),
                moderationResult.checked(),
                sentimentResult.score(),
                sentimentResult.magnitude(),
                sentimentResult.analyzed()
        );
    }

    private LocalSentiment analyzeSentiment(String comment) {
        try {
            AzureLanguageSentimentApiClient.SentimentResult azure =
                    azureLanguageSentimentApiClient.analyzeSentiment(comment, "fr");
            if (azure.analyzed()) {
                return new LocalSentiment(azure.score(), azure.magnitude(), azure.label(), true);
            }
        } catch (ApiException ignored) {
            // Fallback to local scoring when Azure sentiment is unavailable.
        }
        return analyzeLocalSentiment(comment);
    }

    private LocalSentiment analyzeLocalSentiment(String comment) {
        if (comment == null || comment.isBlank()) {
            return new LocalSentiment(0.0, 0.0, "NEU", false);
        }

        String normalized = comment.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.isBlank()) {
            return new LocalSentiment(0.0, 0.0, "NEU", false);
        }

        int positive = 0;
        int negative = 0;
        String[] tokens = normalized.split(" ");
        for (String token : tokens) {
            if (POSITIVE_TERMS.contains(token)) {
                positive++;
            } else if (NEGATIVE_TERMS.contains(token)) {
                negative++;
            }
        }

        double raw = positive - negative;
        double score = Math.max(-1.0, Math.min(1.0, raw / 3.0));
        double magnitude = Math.min(1.0, (positive + negative) / 6.0);
        String tag = score > 0.25 ? "P" : score < -0.25 ? "N" : "NEU";
        return new LocalSentiment(score, magnitude, tag, true);
    }

    private record LocalSentiment(double score, double magnitude, String scoreTag, boolean analyzed) {
    }

    public record ReviewProcessingResult(String finalComment,
                                         boolean moderated,
                                         String moderationReason,
                                         boolean moderationChecked,
                                         double sentimentScore,
                                         double sentimentMagnitude,
                                         boolean sentimentAnalyzed) {
    }
}
