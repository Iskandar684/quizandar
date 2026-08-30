package ru.iskandar.quizandar.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ru.iskandar.quizandar.backend.service.GameService;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GameService gameService;

    // Игрок отправляет ответ: /app/game/{roomId}/answer
    @MessageMapping("/game/{roomId}/answer")
    public void answer(@DestinationVariable String roomId,
                       @Payload Map<String, String> payload,
                       Principal principal) {
        String playerId = principal.getName(); // имя пользователя из STOMP (пока может быть null)
        String selectedOptionId = payload.get("optionId");
        // Если principal не настроен, получаем playerId из payload временно
        if (playerId == null) {
            playerId = payload.get("playerId");
        }
        gameService.submitAnswer(playerId, selectedOptionId);
    }

    // Ведущий запрашивает следующий вопрос: /app/game/{roomId}/next
    @MessageMapping("/game/{roomId}/next")
    public void nextQuestion(@DestinationVariable String roomId) {
        gameService.nextQuestion();
    }

    // Ведущий начинает игру: /app/game/{roomId}/start
    @MessageMapping("/game/{roomId}/start")
    public void startGame(@DestinationVariable String roomId) {
        gameService.startGame();
    }
}
