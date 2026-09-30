package com.cesarmillan.kairos.comment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommentRepository extends MongoRepository<Comment, String> {

    List<Comment> findByShowIdOrderByCreatedAtAscIdAsc(Integer showId);

    List<Comment> findByShowIdInOrderByCreatedAtAscIdAsc(Collection<Integer> showIds);
}
