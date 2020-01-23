package eosio.history.rest;

import javax.validation.constraints.NotNull;

public class Key {

  private String public_key;

    public String getPublic_key() {
        return public_key;
    }

    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }
}
