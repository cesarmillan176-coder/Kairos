package com.cesarmillan.kairos.comment;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document("comments")
@CompoundIndex(name = "show_comments", def = "{'showId': 1, 'createdAt': 1, '_id': 1}")
public record Comment(@Id String id, Integer showId, String comment,
                      @Field(targetType = FieldType.DECIMAL128) BigDecimal rating, Instant createdAt) {
}
