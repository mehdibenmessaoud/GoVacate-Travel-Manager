package tn.esprit.projet.API.images;

import tn.esprit.projet.API.common.ApiException;

public class ImagePipelineService {

    private final CloudinaryApiClient cloudinaryApiClient;
    private final SightengineImageModerationApiClient sightengineImageModerationApiClient;

    public ImagePipelineService() {
        this.cloudinaryApiClient = new CloudinaryApiClient();
        this.sightengineImageModerationApiClient = new SightengineImageModerationApiClient();
    }

    public ImageProcessingResult processImage(String imageInput) {
        return processImage(imageInput, "misc", true, true);
    }

    public ImageProcessingResult processImage(String imageInput, boolean blockOnUnsafeModeration) {
        return processImage(imageInput, "misc", blockOnUnsafeModeration, true);
    }

    public ImageProcessingResult processImage(String imageInput,
                                              boolean blockOnUnsafeModeration,
                                              boolean uploadToCloudinary) {
        return processImage(imageInput, "misc", blockOnUnsafeModeration, uploadToCloudinary);
    }

    public ImageProcessingResult processImage(String imageInput,
                                              String cloudinaryFolder,
                                              boolean blockOnUnsafeModeration,
                                              boolean uploadToCloudinary) {
        String safeInput = imageInput == null ? "" : imageInput.trim();
        if (safeInput.isEmpty()) {
            return new ImageProcessingResult("", false, false, false, "Image path is empty.");
        }

        boolean moderationChecked = false;
        boolean safe = true;
        boolean uploadedToCloudinary = false;
        String note = "Saved with local path.";
        String finalUrl = safeInput;

        if (sightengineImageModerationApiClient.isConfigured()) {
            try {
                SightengineImageModerationApiClient.ModerationResult safety = sightengineImageModerationApiClient.moderateImage(safeInput);
                moderationChecked = true;
                safe = safety.safe();
                if (!safe) {
                    if (blockOnUnsafeModeration) {
                        return new ImageProcessingResult(safeInput, true, false, false,
                                "Blocked by image moderation: " + safety.summary());
                    }
                }
            } catch (ApiException e) {
                note = "Image moderation skipped: " + e.getMessage();
            }
        }

        if (uploadToCloudinary) {
            try {
                CloudinaryApiClient.UploadResult upload = cloudinaryApiClient.uploadImage(safeInput, cloudinaryFolder);
                finalUrl = upload.secureUrl();
                uploadedToCloudinary = upload.uploadedToCloudinary();
                note += " Uploaded to Cloudinary.";
            } catch (ApiException e) {
                note += " Cloudinary upload failed: " + e.getMessage();
            }
        }
        return new ImageProcessingResult(finalUrl, moderationChecked, safe, uploadedToCloudinary, note);
    }

    public record ImageProcessingResult(String finalImageUrl,
                                        boolean moderationChecked,
                                        boolean safe,
                                        boolean uploadedToCloudinary,
                                        String note) {
    }
}
