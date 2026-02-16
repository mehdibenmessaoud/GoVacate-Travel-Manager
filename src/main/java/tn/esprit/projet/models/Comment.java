package tn.esprit.projet.models;
import java.util.Date;
public class Comment {
    private int id;
    private String content;
    private Date createdat;
    private Date updatedat;
    private int userid;
    private int postid;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedat() {
        return createdat;
    }

    public void setCreatedat(Date createdat) {
        this.createdat = createdat;
    }

    public Date getUpdatedat() {
        return updatedat;
    }

    public void setUpdatedat(Date updatedat) {
        this.updatedat = updatedat;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getPostid() {
        return postid;
    }

    public void setPostid(int postid) {
        this.postid = postid;
    }

    public Comment(){};

    public Comment(int id, String content, Date createdat, Date updatedat, int userid, int postid) {
        this.id = id;
        this.content = content;
        this.createdat = createdat;
        this.updatedat = updatedat;
        this.userid = userid;
        this.postid = postid;
    }
}
