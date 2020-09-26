package com.example.clientserver.fragment1andItsRecyclerAdapter;

public interface updater {
    void add(String id,String name,int noOfMessages,String image,int index);
    void notifySingleDataUpdate(String rid, int nom);
}
