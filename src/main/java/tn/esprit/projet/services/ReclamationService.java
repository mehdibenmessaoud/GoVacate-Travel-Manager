package tn.esprit.projet.services;
import tn.esprit.projet.models.Reclamation;
import tn.esprit.projet.utils.govacate_connect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ReclamationService implements CRUD <Reclamation> {
    private Connection cnx;
    public ReclamationService(Connection cnx) {
        this.cnx = cnx;
    }
    public ReclamationService() {
        this.cnx = govacate_connect.getInstance().getConnection();
    }
    @Override
    public void insert(Reclamation r) throws SQLException {
        // AJOUT de postid dans la requête
        String req = "INSERT INTO reclamation (subject, description, userid, postid) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, r.getSubject());
        ps.setString(2, r.getDescription());
        ps.setInt(3, r.getUserid());

        // FIX : On envoie l'ID du blogue récupéré depuis l'objet Reclamation
        ps.setInt(4, r.getPostid());

        ps.executeUpdate();
        System.out.println("Reclamation ajoutee avec succes pour le blog ID: " + r.getPostid());
    }

    @Override
    public void update(Reclamation r) throws SQLException {
        String req = "UPDATE reclamation SET subject = ?, description = ? WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, r.getSubject());
        ps.setString(2, r.getDescription());
        ps.setInt(3, r.getId());
        int rowsAffected = ps.executeUpdate();
        if (rowsAffected == 0) {
            System.out.println("Echec : Aucune reclamation trouvee avec cet ID.");
        } else {
            System.out.println("Succes : Reclamation mise a jour !");
        }
    }

    @Override
    public void delete(Reclamation r) throws SQLException {
        String req = "DELETE FROM reclamation WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, r.getUserid());
        ps.executeUpdate();
    }

    @Override
    public List<Reclamation> selectAll(Reclamation reclamation) throws SQLException {
        return List.of();
    }
}