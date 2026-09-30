package com.cesarmillan.kairos.show;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(Integer showId) {
        super("No se encontró el show con ID " + showId);
    }
}
