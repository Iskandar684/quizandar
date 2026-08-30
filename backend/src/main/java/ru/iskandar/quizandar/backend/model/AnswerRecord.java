package ru.iskandar.quizandar.backend.model;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(prefix = "_")
public class AnswerRecord {
    private String _playerId;
    private String _selectedOptionId;
    private long _timeTakenMs;      // сколько мс прошло с начала вопроса
    private boolean _correct;
    private int _pointsAwarded;     // начисленные баллы
}
