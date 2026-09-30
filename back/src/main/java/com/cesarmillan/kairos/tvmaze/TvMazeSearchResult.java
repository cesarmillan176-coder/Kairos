package com.cesarmillan.kairos.tvmaze;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TvMazeSearchResult(Show show) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Show(Integer id, String name, Channel network, Channel webChannel,
                       String summary, List<String> genres) {

        public String channel() {
            if (network != null && network.name() != null) {
                return network.name();
            }
            return webChannel == null ? null : webChannel.name();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(String name) {
    }
}
