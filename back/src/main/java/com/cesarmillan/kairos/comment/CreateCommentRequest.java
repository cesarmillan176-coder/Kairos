package com.cesarmillan.kairos.comment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCommentRequest(
        @JsonProperty("show_id") @NotNull @Positive Integer showId,
        @NotBlank @Size(max = 2000) String comment,
        @NotNull @DecimalMin("0") @DecimalMax("5") BigDecimal rating) {
}
