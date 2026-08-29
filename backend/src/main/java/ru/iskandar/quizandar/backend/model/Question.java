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
public class Question {

    @NonNull
    @JsonProperty("id")
    private String _id;

    @NonNull
    @JsonProperty("text")
    private String _text;

    @NonNull
    @JsonProperty("type")
    private QuestionType _type;

    @JsonProperty("timeLimitSec")
    private int _timeLimitSec; // 0 = без ограничения

    @NonNull
    @JsonProperty("options")
    private List<Option> _options;

    @NonNull
    @JsonProperty("correctOptionIds")
    private List<String> _correctOptionIds;
}
