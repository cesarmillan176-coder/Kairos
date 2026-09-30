package com.cesarmillan.kairos.show;

import com.cesarmillan.kairos.comment.Comment;
import com.cesarmillan.kairos.comment.CommentRepository;
import com.cesarmillan.kairos.tvmaze.TvMazeClient;
import com.cesarmillan.kairos.tvmaze.TvMazeSearchResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock
    private TvMazeClient tvMazeClient;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ShowService showService;

    @Test
    void returnsCachedShowsWithoutCallingTvMazeOrChangingTheCache() {
        var data = new LinkedHashMap<String, Object>(Map.of("id", 1, "name", "Girls"));
        when(showRepository.findById(1)).thenReturn(Optional.of(new CachedShow(1, data)));
        when(commentRepository.findByShowIdOrderByCreatedAtAscIdAsc(1)).thenReturn(List.of());

        assertThat(showService.getShow(1)).containsEntry("comments", List.of());
        assertThat(data).doesNotContainKey("comments");
        verifyNoInteractions(tvMazeClient);
    }

    @Test
    void savesNewShowsBeforeReturningThem() {
        Map<String, Object> data = Map.of("id", 1, "name", "Girls");
        when(showRepository.findById(1)).thenReturn(Optional.empty());
        when(tvMazeClient.getShow(1)).thenReturn(data);
        when(showRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(showService.findOrFetch(1)).isEqualTo(data);
        verify(showRepository).save(new CachedShow(1, data));
    }

    @Test
    void doesNotReturnSuccessWhenTheCacheCannotBeSaved() {
        when(showRepository.findById(1)).thenReturn(Optional.empty());
        when(tvMazeClient.getShow(1)).thenReturn(Map.of("id", 1));
        when(showRepository.save(any())).thenThrow(new DataAccessResourceFailureException("Unavailable"));

        assertThatThrownBy(() -> showService.getShow(1))
                .isInstanceOf(DataAccessResourceFailureException.class);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void loadsCommentsInOneQueryAndKeepsTheSearchOrder() {
        when(tvMazeClient.search("girls")).thenReturn(List.of(result(2), result(1)));
        when(commentRepository.findByShowIdInOrderByCreatedAtAscIdAsc(List.of(2, 1)))
                .thenReturn(List.of(new Comment("a", 1, "Buena serie", BigDecimal.valueOf(4), Instant.now())));

        var results = showService.search("  girls  ");

        assertThat(results).extracting(ShowSummary::id).containsExactly(2, 1);
        assertThat(results.getFirst().comments()).isEmpty();
        assertThat(results.getLast().comments()).singleElement()
                .satisfies(comment -> assertThat(comment.comment()).isEqualTo("Buena serie"));
        assertThat(results.getFirst().channel()).isNull();
        verify(commentRepository).findByShowIdInOrderByCreatedAtAscIdAsc(List.of(2, 1));
        verifyNoMoreInteractions(commentRepository);
        verifyNoInteractions(showRepository);
    }

    @Test
    void skipsTheDatabaseWhenSearchHasNoMatches() {
        when(tvMazeClient.search("unknown")).thenReturn(List.of());

        assertThat(showService.search("unknown")).isEmpty();
        verifyNoInteractions(showRepository, commentRepository);
    }

    private TvMazeSearchResult result(Integer id) {
        return new TvMazeSearchResult(new TvMazeSearchResult.Show(id, "Show " + id,
                null, null, null, List.of("Drama")));
    }
}
