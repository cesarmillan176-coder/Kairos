package com.cesarmillan.kairos.show;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @GetMapping({"/search", "/api/search"})
    public List<ShowSummary> search(
            @RequestParam("search_query") @NotBlank @Size(max = 200) String query) {
        return showService.search(query);
    }

    @GetMapping({"/show", "/api/show"})
    public Map<String, Object> getShow(@RequestParam("show_id") @Positive Integer showId) {
        return showService.getShow(showId);
    }
}
