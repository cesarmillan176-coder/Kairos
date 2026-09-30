package com.cesarmillan.kairos.error;

import com.cesarmillan.kairos.show.ShowNotFoundException;
import com.cesarmillan.kairos.tvmaze.TvMazeException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ShowNotFoundException.class)
    ProblemDetail handleShowNotFound(ShowNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(TvMazeException.class)
    ProblemDetail handleTvMaze(TvMazeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(DataAccessException.class)
    ProblemDetail handleDatabase(DataAccessException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "La base de datos no está disponible");
    }
}
