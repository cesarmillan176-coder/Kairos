package com.cesarmillan.kairos.tvmaze;

public class TvMazeException extends RuntimeException {

    public TvMazeException(String message) {
        super(message);
    }

    public TvMazeException(String message, Throwable cause) {
        super(message, cause);
    }
}
