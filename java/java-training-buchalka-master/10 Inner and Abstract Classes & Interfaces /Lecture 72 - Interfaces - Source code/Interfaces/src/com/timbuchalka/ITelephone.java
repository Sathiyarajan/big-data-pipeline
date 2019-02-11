package com.timbuchalka;

/**
 * Created by dev on 27/09/15.
 */
public interface ITelephone {
    void powerOn();
    void dial(int phoneNumber);
    void answer();
    boolean callPhone(int phoneNumber);
    boolean isRinging();
}
