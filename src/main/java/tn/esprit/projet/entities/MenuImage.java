package tn.esprit.projet.entities;

import java.util.Objects;

public class MenuImage {

    private int id;
    private String imageUrl;
    private int menuId;

    public MenuImage() {}

    public MenuImage(int id, String imageUrl, int menuId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.menuId = menuId;
    }

    // Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getMenuId() {
        return menuId;
    }

    public void setMenuId(int menuId) {
        this.menuId = menuId;
    }

    @Override
    public String toString() {
        return "MenuImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", menuId=" + menuId +
                '}';
    }


}
