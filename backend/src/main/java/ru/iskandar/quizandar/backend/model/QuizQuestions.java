package ru.iskandar.quizandar.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Обёртка для корневого объекта файла вопросов.
 * Содержит заголовок, описание и список вопросов.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(prefix = "_")
public class QuizQuestions {

    /** Заголовок набора вопросов (тема). */
    @JsonProperty("title")
    private String _title;

    /** Краткое описание набора. */
    @JsonProperty("description")
    private String _description;

    /** Список вопросов. */
    @NonNull
    @JsonProperty("questions")
    private List<Question> _questions;
}
