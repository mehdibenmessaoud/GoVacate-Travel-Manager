package tn.esprit.projet.services;

import tn.esprit.projet.models.Reclamation;

import java.sql.SQLException;
import java.util.List;

public interface CRUD<T> {
    void insert(T t) throws SQLException;
    void update(T t) throws SQLException;
    void delete(T t) throws SQLException;
    List<T> selectAll(T t) throws SQLException;
}
