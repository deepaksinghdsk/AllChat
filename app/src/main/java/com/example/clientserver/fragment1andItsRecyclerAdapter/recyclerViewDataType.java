package com.example.clientserver.fragment1andItsRecyclerAdapter;

public class recyclerViewDataType {
    private int noOfMessages;
    private String name;
    private String image;
    private String rid;

  public recyclerViewDataType(String name, String rid, String image, int noOfMessages) {
        this.name = name;
        this.rid = rid;
        this.image = image;
        this.noOfMessages = noOfMessages;
    }

    public void setNoOfMessages(int nom) {
        noOfMessages = nom;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getRid() {
        return rid;
    }

    public int getNoOfMessages() {
        return noOfMessages;
    }

    public void setImage(String usersData) {
        image = usersData;
    }
}
