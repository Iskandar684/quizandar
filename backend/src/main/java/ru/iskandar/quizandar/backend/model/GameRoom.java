package ru.iskandar.quizandar.backend.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import ru.iskandar.quizandar.backend.response.PlayerResponse;

@Data
@Slf4j
@Accessors(prefix = "_")
public class GameRoom {

	private final String _id;
	private final Map<String, PlayerResponse> _players = new ConcurrentHashMap<>();
	private final Map<String, Integer> _scores = new ConcurrentHashMap<>();
	private List<Question> _questions = new ArrayList<>();
	private int _currentQuestionIndex = -1;
	private Question _currentQuestion = null;
	private boolean _questionActive = false;
	private long _questionStartTime;
	private final Map<String, AnswerRecord> _currentAnswers = new ConcurrentHashMap<>();

	public GameRoom(String id) {
		_id = id;
	}

	// Добавить игрока
	public void addPlayer(PlayerResponse player) {
		_players.put(player.getId(), player);
		_scores.putIfAbsent(player.getId(), 0);
	}

	// Удалить игрока
	public void removePlayer(String playerId) {
		_players.remove(playerId);
		_scores.remove(playerId);
		_currentAnswers.remove(playerId);
	}

	// Получить текущее число игроков
	public int getPlayerCount() {
		return _players.size();
	}

	// Загрузить список вопросов
	public void setQuestions(List<Question> questions) {
		_questions = new ArrayList<>(questions);
		_currentQuestionIndex = -1;
		_currentQuestion = null;
	}

	// Перейти к следующему вопросу
	public Question nextQuestion() {
		if (_questions.isEmpty())
			return null;
		_currentQuestionIndex++;
		if (_currentQuestionIndex >= _questions.size()) {
			_currentQuestionIndex = _questions.size() - 1;
			_currentQuestion = null;
			return null; // вопросы закончились
		}
		_currentQuestion = _questions.get(_currentQuestionIndex);
		_questionActive = true;
		_questionStartTime = System.currentTimeMillis();
		_currentAnswers.clear();
		return _currentQuestion;
	}

	// Завершить текущий вопрос (заглушка, результаты отдаются отдельно)
	public void endQuestion() {
		_questionActive = false;
	}

	// Проверить ответ и начислить очки
	public AnswerRecord processAnswer(String playerId, String selectedOptionId) {
		log.info("Текущий вопрос: {}", _currentQuestion.getText());
		log.info("Правильные ответы: {}", _currentQuestion.getCorrectOptionIds());
		log.info("Выбранный вариант: {}", selectedOptionId);
		
		if (!_questionActive || _currentQuestion == null)
			return null;
		if (!_players.containsKey(playerId))
			return null;
		if (_currentAnswers.containsKey(playerId))
			return null; // уже ответил

		long timeTaken = System.currentTimeMillis() - _questionStartTime;
		boolean correct = _currentQuestion.getCorrectOptionIds().contains(selectedOptionId);

		// Логика баллов: базово 1000 очков, минус штраф за время (например, 1 очко за
		// 10 мс, макс 10 сек)
		int points = correct ? Math.max(100, 1000 - (int) (timeTaken / 10)) : 0;
		log.info("Правильный? {}", correct);
		if (correct) {
			_scores.merge(playerId, points, Integer::sum);
		}

		AnswerRecord record = AnswerRecord.builder().playerId(playerId).selectedOptionId(selectedOptionId)
				.timeTakenMs(timeTaken).correct(correct).pointsAwarded(points).build();
		_currentAnswers.put(playerId, record);
		return record;
	}

	// Получить результаты текущего вопроса (для отправки)
	public List<AnswerRecord> getCurrentResults() {
		return new ArrayList<>(_currentAnswers.values());
	}

	public boolean allPlayersAnswered() {
		return _currentAnswers.size() >= _players.size();
	}

	public Map<String, Integer> getScores() {
		return Map.copyOf(_scores);
	}

	/**
	 * Сбрасывает все очки игроков.
	 */
	public void resetScores() {
		_scores.clear();
	}

}