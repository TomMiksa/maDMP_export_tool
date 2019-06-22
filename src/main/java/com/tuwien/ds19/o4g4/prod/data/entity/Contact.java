package com.tuwien.ds19.o4g4.prod.data.entity;

public class Contact {

    private String name;
    private String mail;
    private String phone;

    private Contact_Id contact_id;

    public Contact(String name, String mail, String phone, Contact_Id contact_id) {
        this.name = name;
        this.mail = mail;
        this.phone = phone;
        this.contact_id = contact_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Contact_Id getContact_id() {
        return contact_id;
    }

    public void setContact_id(Contact_Id contact_id) {
        this.contact_id = contact_id;
    }
}
