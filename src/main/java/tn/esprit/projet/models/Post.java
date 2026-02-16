package tn.esprit.projet.models;
import java.util.Date;
public class Post {
    private int id;
    private String title;
    private String content;
    private String status;
    private Date createdat;
    private Date updatedat;
    private int userid;
    private int locationid;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public int getLocationid() {
        return locationid;
    }

    public void setLocationid(int locationid) {
        this.locationid = locationid;
    }

    public Post() {}

    public Post(int id, String title, String content, String status, Date createdat, Date updatedat, int userid, int locationid) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdat = createdat;
        this.updatedat = updatedat;
        this.userid = userid;
        this.locationid = locationid;
    }
}
