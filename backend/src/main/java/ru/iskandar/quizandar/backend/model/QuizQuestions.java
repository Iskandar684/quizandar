package ru.iskandar.quizandar.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(prefix = "_")
public class QuizQuestions {

	@NonNull
	@JsonProperty("questions")
	private List<Question> _questions;
}
