package tn.esprit.projet.services;
import java.util.List;

public interface IService<T> {
    void ajouterUser(T t);
    void modifierUser(T t);
    void supprimerUser(int id);
    List<T> recupererUser();
}