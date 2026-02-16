package tn.esprit.projet.services;

import tn.esprit.projet.models.Post;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BlogueService implements CRUD<Post> {

    // Connexion à la base de données
    Connection cnx = govacate_connect.getInstance().getConnection();

    // ========================================================================
    // 1. MÉTHODE SPÉCIFIQUE (Celle que le Contrôleur doit utiliser)
    // ========================================================================
    public void ajouterBlogueAvecPlusieursImages(Post p, List<String> images) throws SQLException {
        // 1. Insert the Blog post first
        String queryBlog = "INSERT INTO blogue (title, content, status, userid, createdat) VALUES (?, ?, ?, ?, NOW())";

        // Use RETURN_GENERATED_KEYS to get the new id_blogue
        PreparedStatement psPost = cnx.prepareStatement(queryBlog, Statement.RETURN_GENERATED_KEYS);
        psPost.setString(1, p.getTitle());
        psPost.setString(2, p.getContent());
        psPost.setString(3, p.getStatus());
        psPost.setInt(4, p.getUserid());
        psPost.executeUpdate();

        ResultSet rs = psPost.getGeneratedKeys();
        if (rs.next()) {
            int newBlogId = rs.getInt(1);

            // 2. Insert all images linked to this ID
            String queryImg = "INSERT INTO blogue_images (id_blogue, image_url) VALUES (?, ?)";
            PreparedStatement psImg = cnx.prepareStatement(queryImg);

            for (String url : images) {
                psImg.setInt(1, newBlogId);
                psImg.setString(2, url);
                psImg.addBatch(); // Batch processing for better performance
            }
            psImg.executeBatch();
        }
    }
    public void ajouterBlogueAvecImage(Post post, String imageUrl) {
        // 1. AJOUT DE 'updatedat' DANS LA REQUÊTE SQL
        String reqBlog = "INSERT INTO blogue (title, content, createdat, updatedat, userid, locationid, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = cnx.prepareStatement(reqBlog, Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, post.getTitle());
            pst.setString(2, post.getContent());

            // On récupère la date actuelle une seule fois
            java.sql.Date dateActuelle = new java.sql.Date(System.currentTimeMillis());

            pst.setDate(3, dateActuelle); // createdat
            pst.setDate(4, dateActuelle); // updatedat (Au début, c'est la même date)

            // ATTENTION : On décale les index suivants à cause de l'ajout
            pst.setInt(5, post.getUserid());
            pst.setInt(6, post.getLocationid());
            pst.setString(7, "Publié");

            pst.executeUpdate();

            // ... Le reste du code pour l'image reste identique ...
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                int generatedId = rs.getInt(1);
                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    String reqImg = "INSERT INTO blogue_images (id_blogue, image_url, description) VALUES (?, ?, ?)";
                    PreparedStatement pstImg = cnx.prepareStatement(reqImg);
                    pstImg.setInt(1, generatedId);
                    pstImg.setString(2, imageUrl);
                    pstImg.setString(3, "Image principale");
                    pstImg.executeUpdate();
                }
            }
            System.out.println("Blogue et Image ajoutés avec succès !");

        } catch (SQLException e) {
            System.err.println("Erreur d'ajout (Service) : " + e.getMessage());
        }
    }

    // ========================================================================
    // 2. MÉTHODES DE L'INTERFACE CRUD (Obligatoires)
    // ========================================================================

    @Override
    public void insert(Post post) throws SQLException {
        // Cette méthode implémente l'interface mais n'ajoute pas d'image.
        // Utilisez plutôt 'ajouterBlogueAvecImage' dans votre contrôleur.
        ajouterBlogueAvecImage(post, null);
    }

    @Override
    public void update(Post post) throws SQLException {
        String req = "UPDATE blogue SET title=?, content=?, updatedat=?, status=? WHERE id_blogue=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, post.getTitle());
        pst.setString(2, post.getContent());
        pst.setDate(3, new java.sql.Date(System.currentTimeMillis()));
        pst.setString(4, post.getStatus());
        pst.setInt(5, post.getId());
        pst.executeUpdate();
        System.out.println("Blogue modifié !");
    }

    @Override
    public void delete(Post post) throws SQLException {
        // Supposons que la BDD est en CASCADE DELETE, sinon il faut supprimer les images avant
        String req = "DELETE FROM blogue WHERE id_blogue=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setInt(1, post.getId());
        pst.executeUpdate();
        System.out.println("Blogue supprimé !");
    }

    @Override
    public List<Post> selectAll(Post t) throws SQLException {
        // Retourne la liste brute des objets Post (sans les images jointes)
        List<Post> posts = new ArrayList<>();
        String req = "SELECT * FROM blogue";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Post p = new Post(
                    rs.getInt("id_blogue"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("status"),
                    rs.getDate("createdat"),
                    rs.getDate("updatedat"),
                    rs.getInt("userid"),
                    rs.getInt("locationid")
            );
            posts.add(p);
        }
        return posts;
    }

    // ========================================================================
    // 3. DTO ET AFFICHAGE COMPLEXE (Pour le GUI JavaFX)
    // ========================================================================

    // Classe interne pour transporter les données jointes (Blog + Image)
    public static class BlogPostDTO {
        public String titre;
        public String contenu;
        public String imageUrl;
        public String date;

        public BlogPostDTO(String titre, String contenu, String imageUrl, String date) {
            this.titre = titre;
            this.contenu = contenu;
            this.imageUrl = imageUrl;
            this.date = date;
        }
    }

    // Méthode optimisée pour l'affichage (JOIN)
    public List<BlogPostDTO> recupererBlogsPourAffichage() {
        // CORRECTION ICI : On initialise une nouvelle liste vide
        List<BlogPostDTO> posts = new ArrayList<>();

        // Correction des noms de tables : 'blogue' et 'blogue_images'
        String req = "SELECT b.title, b.content, b.createdat, i.image_url " +
                "FROM blogue b " +
                "LEFT JOIN blogue_images i ON b.id_blogue = i.id_blogue " +
                "GROUP BY b.id_blogue " +
                "ORDER BY b.createdat DESC";

        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);

            while (rs.next()) {
                String titre = rs.getString("title");
                String content = rs.getString("content");
                String url = rs.getString("image_url");
                Date dateSql = rs.getDate("createdat");
                String dateStr = (dateSql != null) ? dateSql.toString() : "Récemment";

                if (url == null) url = "";

                posts.add(new BlogPostDTO(titre, content, url, dateStr));
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération blogs DTO: " + e.getMessage());
        }
        return posts; // On retourne la liste remplie
    }
}