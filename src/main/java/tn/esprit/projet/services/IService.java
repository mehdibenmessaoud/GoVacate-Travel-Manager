package tn.esprit.projet.services;

import java.sql.SQLException;
import java.util.List;

/** Interface générique pour les services CRUD utilisateur. */
public interface IService<T> {
    void ajouterUser(T t) throws SQLException;
    void modifierUser(T t) throws SQLException;
    void supprimerUser(int id) throws SQLException;
    List<T> recupererUser() throws SQLException;
}