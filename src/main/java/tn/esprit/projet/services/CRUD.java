package tn.esprit.projet.services;

import java.sql.SQLException;
import java.util.List;

// Updated to accept both Entity Type (T) and ID Type (ID)
public interface CRUD<T, ID> {
    T insert(T t) throws SQLException;
    T update(T t) throws SQLException;
    void delete(ID id) throws SQLException;
    List<T> selectAll() throws SQLException;
    T getById(ID id) throws SQLException;
}