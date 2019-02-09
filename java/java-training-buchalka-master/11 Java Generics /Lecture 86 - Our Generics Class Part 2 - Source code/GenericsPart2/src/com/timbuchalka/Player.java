package com.timbuchalka;

/**
 * Created by dev on 17/10/2015.
 */
public abstract class Player {
    private String name;

    public Player(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
