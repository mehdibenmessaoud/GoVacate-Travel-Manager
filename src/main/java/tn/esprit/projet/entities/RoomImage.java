package tn.esprit.projet.entities;

/**
 * Represents an image associated with a room.
 */
public class RoomImage {

    private int id;
    private String imageUrl;
    private int roomId;

    public RoomImage() {
    }

    public RoomImage(int id, String imageUrl, int roomId) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.roomId = roomId;
    }

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

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "RoomImage{" +
                "id=" + id +
                ", imageUrl='" + imageUrl + '\'' +
                ", roomId=" + roomId +
                '}';
    }
}
