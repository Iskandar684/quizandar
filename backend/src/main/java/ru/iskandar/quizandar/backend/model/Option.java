package ru.iskandar.quizandar.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(prefix = "_")
public class Option {

	@NonNull
	@JsonProperty("id")
	private String _id;

	@NonNull
	@JsonProperty("text")
	private String _text;
}
