package ru.iskandar.quizandar.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.model.QuizQuestions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

	private final ObjectMapper objectMapper;

	private List<Question> questions = new ArrayList<>();

	@PostConstruct
	public void init() {
		loadQuestionsFromResource("questions/sample-questions.json");
	}

	/**
	 * Загружает вопросы из JSON-файла в classpath.
	 */
	private void loadQuestionsFromResource(String path) {
		try (InputStream is = new ClassPathResource(path).getInputStream()) {
			QuizQuestions quiz = objectMapper.readValue(is, QuizQuestions.class);
			questions = new ArrayList<>(quiz.getQuestions());
			log.info("Загружено {} вопросов из {}", questions.size(), path);
		} catch (IOException e) {
			log.error("Не удалось загрузить вопросы из {}", path, e);
			questions = Collections.emptyList();
		}
	}

	/**
	 * Возвращает список всех вопросов (неизменяемая копия).
	 */
	public List<Question> getAllQuestions() {
		return List.copyOf(questions);
	}

	/**
	 * Возвращает вопрос по id или null.
	 */
	public Question getQuestionById(String id) {
		return questions.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
	}
	
}
