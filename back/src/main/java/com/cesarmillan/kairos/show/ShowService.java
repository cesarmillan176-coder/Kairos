package com.cesarmillan.kairos.show;

import com.cesarmillan.kairos.comment.Comment;
import com.cesarmillan.kairos.comment.CommentRepository;
import com.cesarmillan.kairos.comment.CommentResponse;
import com.cesarmillan.kairos.tvmaze.TvMazeClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ShowService {

    private final TvMazeClient tvMazeClient;
    private final ShowRepository showRepository;
    private final CommentRepository commentRepository;

    public ShowService(TvMazeClient tvMazeClient, ShowRepository showRepository,
                       CommentRepository commentRepository) {
        this.tvMazeClient = tvMazeClient;
        this.showRepository = showRepository;
        this.commentRepository = commentRepository;
    }

    public List<ShowSummary> search(String query) {
        var shows = tvMazeClient.search(query.strip()).stream().map(result -> result.show()).toList();
        if (shows.isEmpty()) {
            return List.of();
        }

        var showIds = shows.stream().map(show -> show.id()).toList();
        var comments = commentRepository.findByShowIdInOrderByCreatedAtAscIdAsc(showIds).stream()
                .collect(Collectors.groupingBy(Comment::showId,
                        Collectors.mapping(CommentResponse::from, Collectors.toList())));

        return shows.stream()
                .map(show -> new ShowSummary(show.id(), show.name(), show.channel(),
                        show.summary(), show.genres() == null ? List.of() : show.genres(),
                        comments.getOrDefault(show.id(), List.of())))
                .toList();
    }

    public Map<String, Object> getShow(Integer showId) {
        var response = new LinkedHashMap<>(findOrFetch(showId));
        var comments = commentRepository.findByShowIdOrderByCreatedAtAscIdAsc(showId).stream()
                .map(CommentResponse::from)
                .toList();
        response.put("comments", comments);
        return response;
    }

    public Map<String, Object> findOrFetch(Integer showId) {
        return showRepository.findById(showId)
                .orElseGet(() -> showRepository.save(new CachedShow(showId, tvMazeClient.getShow(showId))))
                .data();
    }
}
