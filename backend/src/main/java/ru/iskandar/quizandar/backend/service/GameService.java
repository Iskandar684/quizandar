package ru.iskandar.quizandar.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.iskandar.quizandar.backend.model.AnswerRecord;
import ru.iskandar.quizandar.backend.model.GameRoom;
import ru.iskandar.quizandar.backend.model.Option;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.model.QuestionType;
import ru.iskandar.quizandar.backend.response.PlayerResponse;
import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

	private final QuestionService questionService;
	private final SimpMessagingTemplate messagingTemplate;

	private final GameRoom room = new GameRoom("main");

	// Инициализация: загрузка вопросов
	@PostConstruct
	public void init() {
		room.setQuestions(questionService.getAllQuestions());
	}

	// Добавить игрока (вызывается после регистрации)
	public void addPlayer(PlayerResponse player) {
		room.addPlayer(player);
		// Отправить обновлённый список игроков ведущему
		sendToHost("/topic/game/players", room.getPlayers().values());
	}

	// Начать игру: сбросить очки и начать с первого вопроса
	public void startGame() {
		room.resetScores();
		room.setQuestions(questionService.getAllQuestions());
		room.nextQuestion(); // загрузить первый вопрос
		broadcastQuestion();
	}

	// Отправить текущий вопрос всем игрокам
	private void broadcastQuestion() {
		Question q = room.getCurrentQuestion();
		if (q != null) {
			// Отправляем копию без правильных ответов!
			QuestionDto questionDto = toDto(q);
			messagingTemplate.convertAndSend("/topic/game/question", questionDto);
			sendToHost("/topic/game/status", Map.of("active", true));
		}
	}

	// Обработать ответ игрока
	public void submitAnswer(String playerId, String selectedOptionId) {
		log.info("Получен ответ: playerId={}, optionId={}", playerId, selectedOptionId);
		AnswerRecord record = room.processAnswer(playerId, selectedOptionId);
		if (record != null) {
			log.info("Ответ обработан: {}", record);
			// Подтверждение игроку
			messagingTemplate.convertAndSend("/topic/game/player/" + playerId + "/result", record);
			// Если все ответили, отправить результаты ведущему
			if (room.allPlayersAnswered()) {
				room.endQuestion();
				sendResultsToAll();
			}
		} else {
			log.warn("Ответ не обработан (возможно, уже отвечал или вопрос не активен)");
		}
	}

	// Отправить результаты текущего вопроса всем
	private void sendResultsToAll() {
		List<AnswerRecord> results = room.getCurrentResults();
		messagingTemplate.convertAndSend("/topic/game/results", results);
		// Также обновить общий счёт
		messagingTemplate.convertAndSend("/topic/game/scores", room.getScores());
		sendToHost("/topic/game/status", Map.of("active", false));
	}

	// Перейти к следующему вопросу (по команде ведущего)
	public void nextQuestion() {
		Question q = room.nextQuestion();
		if (q != null) {
			broadcastQuestion();
		} else {
			// Вопросы закончились — отправить финальные результаты
			messagingTemplate.convertAndSend("/topic/game/final-scores", room.getScores());
			sendToHost("/topic/game/status", Map.of("finished", true));
		}
	}

	// Вспомогательный метод для отправки сообщения ведущему (используем отдельный
	// топик)
	private void sendToHost(String destination, Object payload) {
		messagingTemplate.convertAndSend(destination, payload);
	}

	// Преобразование Question в DTO без правильных ответов
	private QuestionDto toDto(Question q) {
		// Создаём DTO для клиента
		return QuestionDto.builder().id(q.getId()).text(q.getText()).type(q.getType()).timeLimitSec(q.getTimeLimitSec())
				.options(q.getOptions()).build();
	}

	// DTO для вопроса (без correctOptionIds) — можно вынести в отдельный класс
	@lombok.Data
	@lombok.Builder
	@lombok.NoArgsConstructor
	@lombok.AllArgsConstructor
	@lombok.experimental.Accessors(prefix = "_")
	public static class QuestionDto {
		private String _id;
		private String _text;
		private QuestionType _type;
		private int _timeLimitSec;
		private List<Option> _options;
	}
}
