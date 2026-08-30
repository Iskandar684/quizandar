package ru.iskandar.quizandar.backend.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import ru.iskandar.quizandar.backend.service.GameService;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

	private final GameService gameService;

	// Игрок отправляет ответ: /app/game/{roomId}/answer
	@MessageMapping("/game/{roomId}/answer")
	public void answer(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
		String playerId = payload.get("playerId");
		String selectedOptionId = payload.get("optionId");
		if (playerId == null || selectedOptionId == null) {
			// Игнорируем некорректные сообщения
			return;
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
