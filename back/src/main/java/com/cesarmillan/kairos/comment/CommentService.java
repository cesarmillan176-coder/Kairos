package com.cesarmillan.kairos.comment;

import com.cesarmillan.kairos.show.ShowService;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ShowService showService;

    public CommentService(CommentRepository commentRepository, ShowService showService) {
        this.commentRepository = commentRepository;
        this.showService = showService;
    }

    public void create(CreateCommentRequest request) {
        showService.findOrFetch(request.showId());
        commentRepository.save(new Comment(null, request.showId(), request.comment().strip(),
                request.rating(), Instant.now()));
    }
}
