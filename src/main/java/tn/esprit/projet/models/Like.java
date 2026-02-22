package tn.esprit.projet.models;
import java.util.Date;
public class Like {
    private int id;
    private int user_id;
    private int postid;
    private Date createdat; ;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public int getPostid() {
        return postid;
    }

    public void setPostid(int postid) {
        this.postid = postid;
    }

    public Date getCreatedat() {
        return createdat;
    }

    public void setCreatedat(Date createdat) {
        this.createdat = createdat;
    }
    public Like(){};
    public Like(int idBlogue, int userid){};

    public Like(int id, int user_id, int postid, Date createdat) {
        this.id = id;
        this.user_id = user_id;
        this.postid = postid;
        this.createdat = createdat;
    }
}
