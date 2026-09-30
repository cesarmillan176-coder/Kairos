package com.cesarmillan.kairos.comment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping({"/comments", "/api/comments"})
    @ResponseStatus(HttpStatus.CREATED)
    public CommentStatus create(@Valid @RequestBody CreateCommentRequest request) {
        commentService.create(request);
        return new CommentStatus("created");
    }

    public record CommentStatus(String status) {
    }
}
