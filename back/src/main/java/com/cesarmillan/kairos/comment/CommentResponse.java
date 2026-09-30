package com.cesarmillan.kairos.comment;

import java.math.BigDecimal;

public record CommentResponse(String comment, BigDecimal rating) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.comment(), comment.rating());
    }
}
