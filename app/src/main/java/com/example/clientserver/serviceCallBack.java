package com.example.clientserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;

interface serviceCallBack {
   void init(DataInputStream is, DataOutputStream os);
   void setMessage(String message);
}
