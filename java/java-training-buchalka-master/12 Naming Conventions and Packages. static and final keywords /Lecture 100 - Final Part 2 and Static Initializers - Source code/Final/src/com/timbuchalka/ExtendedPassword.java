package com.timbuchalka;

/**
 * Created by dev on 23/11/2015.
 */
public class ExtendedPassword extends Password {
    private int decryptedPassword;

    public ExtendedPassword(int password) {
        super(password);
        this.decryptedPassword = password;
    }

//    @Override
//    public void storePassword() {
//        System.out.println("Saving password as " + this.decryptedPassword);
//    }
}
