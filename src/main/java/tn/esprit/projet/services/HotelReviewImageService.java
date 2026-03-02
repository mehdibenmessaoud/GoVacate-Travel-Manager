package tn.esprit.projet.services;

import tn.esprit.projet.entities.HotelReviewImage;
import tn.esprit.projet.API.images.ImagePipelineService;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelReviewImageService implements IService<HotelReviewImage> {

    private Connection cnx;
    private final ImagePipelineService imagePipelineService;

    public HotelReviewImageService() {
        cnx = MyDBConnexion.getInstance().getConnection();
        imagePipelineService = new ImagePipelineService();
    }

    @Override
    public void create(HotelReviewImage image) throws SQLException {
        String sql = "INSERT INTO hotel_review_image (imageUrl, hotelReviewId) VALUES (?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getHotelReviewId());

        ps.executeUpdate();
    }

    @Override
    public List<HotelReviewImage> getAll() throws SQLException {
        List<HotelReviewImage> images = new ArrayList<>();
        String sql = "SELECT * FROM hotel_review_image";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            images.add(new HotelReviewImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelReviewId")
            ));
        }

        return images;
    }

    @Override
    public void update(HotelReviewImage image) throws SQLException {
        String sql = "UPDATE hotel_review_image SET imageUrl=?, hotelReviewId=? WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, image.getImageUrl());
        ps.setInt(2, image.getHotelReviewId());
        ps.setInt(3, image.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM hotel_review_image WHERE id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public HotelReviewImage getById(int id) throws SQLException {
        String sql = "SELECT * FROM hotel_review_image WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new HotelReviewImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelReviewId")
            );
        }

        return null;
    }

    // Get all images for a specific review
    public List<HotelReviewImage> getByReviewId(int hotelReviewId) throws SQLException {
        List<HotelReviewImage> images = new ArrayList<>();
        String sql = "SELECT * FROM hotel_review_image WHERE hotelReviewId = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, hotelReviewId);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            images.add(new HotelReviewImage(
                    rs.getInt("id"),
                    rs.getString("imageUrl"),
                    rs.getInt("hotelReviewId")
            ));
        }
        return images;
    }

    public HotelReviewImage createWithImagePipeline(String imageInput, int hotelReviewId) throws SQLException {
        ImagePipelineService.ImageProcessingResult result = imagePipelineService.processImage(imageInput, "reviews", true, true);
        if (!result.moderationChecked()) {
            throw new IllegalArgumentException("Verification de securite indisponible. Reessayez dans quelques instants.");
        }
        if (!result.safe()) {
            throw new IllegalArgumentException("Image refusee: contenu non conforme a la politique de la plateforme.");
        }
        if (!result.uploadedToCloudinary()) {
            throw new IllegalArgumentException("Service de stockage cloud temporairement indisponible. Reessayez.");
        }
        HotelReviewImage image = new HotelReviewImage(0, result.finalImageUrl(), hotelReviewId);
        create(image);
        return image;
    }
}
