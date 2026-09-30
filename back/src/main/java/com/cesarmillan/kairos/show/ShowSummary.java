package com.cesarmillan.kairos.show;

import com.cesarmillan.kairos.comment.CommentResponse;
import java.util.List;

public record ShowSummary(Integer id, String name, String channel, String summary,
                          List<String> genres, List<CommentResponse> comments) {
}
