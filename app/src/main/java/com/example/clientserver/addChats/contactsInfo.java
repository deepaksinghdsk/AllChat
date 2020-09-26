package com.example.clientserver.addChats;

public class contactsInfo {
    private String name, number, previousGroups = "", id, previousOldGroups;
    private boolean checked = false, userExists = false;

    public contactsInfo(String name, String number) {
        this.name = name;
        this.number = number;
    }

    contactsInfo(String name, String number, String previousGroups,String previousOldGroups, String id) {
        this.name = name;
        this.number = number;
        this.previousGroups = previousGroups == null || previousGroups.equals("null") ? "" : previousGroups;
        this.previousOldGroups = previousOldGroups == null || previousOldGroups.equals("null") ? "" : previousOldGroups;
        this.id = id;
    }

    String getName() {
        return name;
    }

    String getNumber() {
        return number;
    }

    String getPreviousGroups() {
        return previousGroups;
    }

    String getPreviousOldGroups() {
        return previousOldGroups;
    }

    boolean getChecked() {
        return checked;
    }

    void setChecked(boolean checked) {
        this.checked = checked;
    }

    boolean getUserExists() {
        return userExists;
    }

    void setUserExists(boolean userExists) {
        this.userExists = userExists;
    }

    String getId() {
        return id;
    }
}
