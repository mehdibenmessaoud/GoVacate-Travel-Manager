package tn.esprit.projet.services;

import java.util.List;

public interface CrudService<T, ID> {

    T create(T entity);

    T update(T entity);

    void delete(ID id);

    T findById(ID id);

    List<T> findAll();
}
