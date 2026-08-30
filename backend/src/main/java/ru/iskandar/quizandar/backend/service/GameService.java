package ru.iskandar.quizandar.backend.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.iskandar.quizandar.backend.model.AnswerRecord;
import ru.iskandar.quizandar.backend.model.GameRoom;
import ru.iskandar.quizandar.backend.model.Option;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.model.QuestionType;
import ru.iskandar.quizandar.backend.response.PlayerResponse;

/**
 * Сервис управления игровым процессом. Хранит состояние комнаты, обрабатывает
 * ответы, управляет таймерами и автоматическим переходом к следующему вопросу.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

	/** Шаблон для отправки сообщений через WebSocket. */
	private final SimpMessagingTemplate messagingTemplate;

	/** Сервис загрузки вопросов. */
	private final QuestionService questionService;

	/** Игровая комната (одна на приложение). */
	private final GameRoom room = new GameRoom("main");

	/** Планировщик задач для таймеров вопроса и задержек. */
	private ScheduledExecutorService scheduler;

	/** Будущая задача таймаута текущего вопроса. */
	private ScheduledFuture<?> questionTimeoutFuture;

	/** Будущая задача отложенного перехода к следующему вопросу. */
	private ScheduledFuture<?> nextQuestionFuture;

	/**
	 * Флаг автоматического перехода к следующему вопросу (по умолчанию включён).
	 */
	private volatile boolean autoNext = true;

	/**
	 * Задержка перед автоматическим переходом (в секундах) для просмотра
	 * результатов.
	 */
	private static final long AUTO_NEXT_DELAY_SECONDS = 5;

	/**
	 * Инициализация сервиса: создание планировщика и загрузка вопросов.
	 */
	@PostConstruct
	public void init() {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		room.setQuestions(questionService.getAllQuestions());
		log.info("GameService инициализирован, загружено вопросов: {}", room.getQuestions().size());
	}

	/**
	 * Останавливает планировщик при завершении работы приложения.
	 */
	@PreDestroy
	public void shutdown() {
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

	/**
	 * Устанавливает флаг автоматического перехода.
	 *
	 * @param value true, если нужно автоматически переходить к следующему вопросу
	 */
	public void setAutoNext(boolean value) {
		this.autoNext = value;
		log.info("Автопереход установлен в {}", value);
	}

	/**
	 * Добавляет игрока в комнату и уведомляет ведущего об обновлении списка.
	 *
	 * @param player данные игрока
	 */
	public void addPlayer(PlayerResponse player) {
		room.addPlayer(player);
		sendToHost("/topic/game/players", room.getPlayers().values());
	}

	/**
	 * Запускает игру: сбрасывает очки, загружает вопросы и показывает первый
	 * вопрос.
	 */
	public void startGame() {
		room.resetScores();
		room.setQuestions(questionService.getAllQuestions());
		room.nextQuestion(); // переключиться на первый вопрос (индекс станет 0)
		broadcastQuestion();
	}

	/**
	 * Переходит к следующему вопросу и рассылает его игрокам. Если вопросы
	 * закончились, отправляет финальные результаты.
	 */
	public void nextQuestion() {
		cancelPendingTasks(); // отменяем все отложенные задачи
		Question q = room.nextQuestion();
		if (q != null) {
			broadcastQuestion();
		} else {
			sendFinalScores();
		}
	}

	/**
	 * Обрабатывает ответ игрока.
	 *
	 * @param playerId         идентификатор игрока
	 * @param selectedOptionId выбранный вариант ответа
	 */
	public void submitAnswer(String playerId, String selectedOptionId) {
		AnswerRecord record = room.processAnswer(playerId, selectedOptionId);
		if (record != null) {
			log.info("Ответ обработан: {}", record);
			// Отправляем персональный результат игроку
			messagingTemplate.convertAndSend("/topic/game/player/" + playerId + "/result", record);

			// Если все ответили, завершаем вопрос и переходим дальше
			if (room.allPlayersAnswered()) {
				finishQuestionAndProceed();
			}
		} else {
			log.warn("Ответ не обработан (возможно, уже отвечал или вопрос не активен)");
		}
	}

	/**
	 * Отправляет текущий вопрос всем игрокам и запускает таймер времени.
	 */
	private void broadcastQuestion() {
		Question q = room.getCurrentQuestion();
		if (q == null)
			return;

		// Отправляем вопрос без правильных ответов
		messagingTemplate.convertAndSend("/topic/game/question", toDto(q));
		messagingTemplate.convertAndSend("/topic/game/status", (Object) Map.of("active", true));

		// Запускаем таймер окончания вопроса
		if (q.getTimeLimitSec() > 0) {
			scheduleQuestionTimeout(q.getTimeLimitSec());
		}
	}

	/**
	 * Планирует задачу таймаута вопроса.
	 *
	 * @param timeLimitSec лимит времени в секундах
	 */
	private void scheduleQuestionTimeout(int timeLimitSec) {
		cancelQuestionTimeout();
		questionTimeoutFuture = scheduler.schedule(() -> {
			log.info("Время вопроса истекло, завершаем...");
			if (room.isQuestionActive()) {
				finishQuestionAndProceed();
			}
		}, timeLimitSec, TimeUnit.SECONDS);
	}

	/**
	 * Завершает текущий вопрос: отменяет таймер, отправляет результаты, и если это
	 * последний вопрос — отправляет финальные результаты, иначе планирует переход к
	 * следующему (если autoNext).
	 */
	private void finishQuestionAndProceed() {
		cancelQuestionTimeout();
		room.endQuestion();

		// Отправляем результаты текущего вопроса всем
		List<AnswerRecord> results = room.getCurrentResults();
		messagingTemplate.convertAndSend("/topic/game/results", results);
		messagingTemplate.convertAndSend("/topic/game/scores", room.getScores());
		messagingTemplate.convertAndSend("/topic/game/status", (Object) Map.of("active", false));

		// Если это был последний вопрос — отправляем финальные результаты
		if (room.getCurrentQuestionIndex() == room.getQuestions().size() - 1) {
			sendFinalScores();
			return;
		}

		// Если автопереход включён и есть ещё вопросы, планируем следующий
		if (autoNext) {
			scheduleNextQuestion();
		}
	}

	/**
	 * Планирует переход к следующему вопросу с задержкой для просмотра результатов.
	 */
	private void scheduleNextQuestion() {
		cancelNextQuestionFuture();
		nextQuestionFuture = scheduler.schedule(() -> {
			log.info("Автопереход к следующему вопросу...");
			nextQuestion();
		}, AUTO_NEXT_DELAY_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * Отправляет финальные результаты игры всем участникам.
	 */
	private void sendFinalScores() {
		log.info("Игра завершена, отправляем финальный счёт");
		messagingTemplate.convertAndSend("/topic/game/final-scores", room.getScores());
		messagingTemplate.convertAndSend("/topic/game/status", (Object) Map.of("finished", true));
	}

	/**
	 * Отменяет все запланированные задачи (таймер вопроса и отложенный переход).
	 */
	private void cancelPendingTasks() {
		cancelQuestionTimeout();
		cancelNextQuestionFuture();
	}

	/**
	 * Отменяет задачу таймаута вопроса, если она была запланирована.
	 */
	private void cancelQuestionTimeout() {
		if (questionTimeoutFuture != null && !questionTimeoutFuture.isDone()) {
			questionTimeoutFuture.cancel(false);
			questionTimeoutFuture = null;
		}
	}

	/**
	 * Отменяет задачу отложенного перехода к следующему вопросу.
	 */
	private void cancelNextQuestionFuture() {
		if (nextQuestionFuture != null && !nextQuestionFuture.isDone()) {
			nextQuestionFuture.cancel(false);
			nextQuestionFuture = null;
		}
	}

	/**
	 * Преобразует Question в DTO для клиента (без правильных ответов).
	 *
	 * @param q исходный вопрос
	 * @return DTO вопроса
	 */
	private QuestionDto toDto(Question q) {
		return QuestionDto.builder().id(q.getId()).text(q.getText()).type(q.getType()).timeLimitSec(q.getTimeLimitSec())
				.options(q.getOptions()).build();
	}

	/**
	 * Отправляет сообщение ведущему через общий топик.
	 *
	 * @param destination адрес назначения
	 * @param payload     данные
	 */
	private void sendToHost(String destination, Object payload) {
		messagingTemplate.convertAndSend(destination, payload);
	}

	/**
	 * DTO вопроса для клиента (внутренний класс).
	 */
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
