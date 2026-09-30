package com.cesarmillan.kairos.show;

import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("shows")
public record CachedShow(@Id Integer id, Map<String, Object> data) {
}
