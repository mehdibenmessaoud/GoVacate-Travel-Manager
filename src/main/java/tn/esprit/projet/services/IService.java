package tn.esprit.projet.services;

import java.util.List;
import java.sql.SQLException;

public interface IService<T> {
    // Créer une nouvelle entrée
    void create(T t) throws SQLException;

    // Récupérer toutes les entrées
    List<T> getAll() throws SQLException;

    // Mettre à jour une entrée existante
    void update(T t) throws SQLException;

    // Supprimer une entrée par son identifiant
    void delete(int id) throws SQLException;

    // Récupérer une seule entrée par son identifiant
    T getById(int id) throws SQLException;


}