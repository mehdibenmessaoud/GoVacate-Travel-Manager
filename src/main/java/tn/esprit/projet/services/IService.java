package tn.esprit.projet.services;

import java.sql.SQLException;
import java.util.List;

/**
 * Generic CRUD interface for all service classes.
 */
public interface CRUD<T> {
    
    void create(T entity) throws SQLException;
    
    List<T> getAll() throws SQLException;
    
    T getById(int id) throws SQLException;
    
    void update(T entity) throws SQLException;
    
    void delete(int id) throws SQLException;
}
