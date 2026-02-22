package tn.esprit.projet.models;

import java.util.Date;

public class Reclamation {
    private int id;
    private String subject;
    private String description;
    private String status;
    private String response;
    private Date createdat;
    private Date resolveat;
    private int userid;
    private int reservationid;
    private int postid;
    private int commentid;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Date getCreatedat() {
        return createdat;
    }

    public void setCreatedat(Date createdat) {
        this.createdat = createdat;
    }

    public Date getResolveat() {
        return resolveat;
    }

    public void setResolveat(Date resolveat) {
        this.resolveat = resolveat;
    }

    public int getUserid() {
        return userid;
    }

    public void setUserid(int userid) {
        this.userid = userid;
    }

    public int getReservationid() {
        return reservationid;
    }

    public void setReservationid(int reservationid) {
        this.reservationid = reservationid;
    }

    public int getPostid() {
        return postid;
    }

    public void setPostid(int postid) {
        this.postid = postid;
    }

    public int getCommentid() {
        return commentid;
    }

    public void setCommentid(int commentid) {
        this.commentid = commentid;
    }

    public Reclamation(String subject, String description,int userid) {
        this.subject = subject;
        this.description = description;
        this.userid = userid;
    }
    public Reclamation(){};
}
