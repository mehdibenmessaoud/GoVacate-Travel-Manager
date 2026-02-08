package tn.esprit.projet.services;

import tn.esprit.projet.entities.User;
import tn.esprit.projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private Connection cnx;

    public UserService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void ajouterUser(User u) {
        String sql = "INSERT INTO utilisateur (nom, email, password, role_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setInt(4, u.getRole_id());
            ps.executeUpdate();
            System.out.println("Utilisateur ajouté !");
        } catch (SQLException e) {
            System.err.println("Erreur d'ajout : " + e.getMessage());
        }
    }

    @Override
    public List<User> recupererUser() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("password"),
                        rs.getInt("role_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur de récupération : " + e.getMessage());
        }
        return users;
    }

    @Override
    public void modifierUser(User u) {
        String sql = "UPDATE utilisateur SET nom=?, email=?, password=?, role_id=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setInt(4, u.getRole_id());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void supprimerUser(int id) {
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM utilisateur WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}