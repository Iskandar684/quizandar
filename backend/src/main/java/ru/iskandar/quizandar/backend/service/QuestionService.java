package ru.iskandar.quizandar.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.model.QuizQuestions;

/**
 * Сервис для работы с вопросами. Загружает вопросы из внешнего каталога
 * (volume) или classpath, поддерживает импорт новых файлов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

	private final ObjectMapper objectMapper;

	/** Путь к каталогу с вопросами (настраивается). */
	@Value("${quizandar.questions.directory:/app/data/questions}")
	private String questionsDirectory;

	/** Текущий список вопросов. */
	private List<Question> questions = new ArrayList<>();

	/**
	 * Инициализация сервиса: загрузка вопросов из каталога.
	 */
	@PostConstruct
	public void init() {
		loadQuestionsFromDirectory();
		// Если внешний каталог пуст, загружаем встроенный пример
		if (questions.isEmpty()) {
			loadQuestionsFromResource("questions/questions-cartoons.json");
		}
	}

	/**
	 * Загружает все JSON-файлы из указанного каталога и объединяет вопросы.
	 */
	private void loadQuestionsFromDirectory() {
		Path dir = Paths.get(questionsDirectory);
		if (!Files.exists(dir)) {
			log.warn("Каталог {} не существует, создаём", dir);
			try {
				Files.createDirectories(dir);
			} catch (IOException e) {
				log.error("Не удалось создать каталог {}", dir, e);
				return;
			}
		}

		List<Question> allQuestions = new ArrayList<>();
		try (var stream = Files.list(dir)) {
			List<Path> files = stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();

			for (Path file : files) {
				try (InputStream is = Files.newInputStream(file)) {
					QuizQuestions quiz = objectMapper.readValue(is, QuizQuestions.class);
					allQuestions.addAll(quiz.getQuestions());
				} catch (IOException e) {
					log.error("Ошибка загрузки файла {}", file, e);
				}
			}
		} catch (IOException e) {
			log.error("Ошибка чтения каталога {}", dir, e);
		}

		questions = allQuestions;
		log.info("Загружено {} вопросов из каталога {}", questions.size(), dir);
	}

	/**
	 * Загружает вопросы из ресурсов classpath.
	 *
	 * @param path путь к файлу
	 */
	private void loadQuestionsFromResource(String path) {
		try (InputStream is = new ClassPathResource(path).getInputStream()) {
			QuizQuestions quiz = objectMapper.readValue(is, QuizQuestions.class);
			questions = new ArrayList<>(quiz.getQuestions());
			log.info("Загружено {} вопросов из classpath {}", questions.size(), path);
		} catch (IOException e) {
			log.error("Не удалось загрузить вопросы из {}", path, e);
			questions = Collections.emptyList();
		}
	}

	/**
	 * Импортирует файл вопросов: сохраняет во внешний каталог и перезагружает
	 * список.
	 *
	 * @param file файл JSON с вопросами
	 * @return количество загруженных вопросов
	 * @throws IOException если произошла ошибка сохранения
	 */
	public int importQuestions(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("Файл пуст");
		}

		// Генерируем уникальное имя файла
		String originalFilename = file.getOriginalFilename();
		String filename = (originalFilename != null && originalFilename.endsWith(".json")) ? originalFilename
				: "imported-" + System.currentTimeMillis() + ".json";

		Path dir = Paths.get(questionsDirectory);
		Files.createDirectories(dir);
		Path target = dir.resolve(filename);
		file.transferTo(target.toAbsolutePath().toFile());

		// Перезагружаем вопросы из каталога
		loadQuestionsFromDirectory();
		return questions.size();
	}

	/**
	 * Возвращает список всех вопросов.
	 *
	 * @return неизменяемая копия списка
	 */
	public List<Question> getAllQuestions() {
		return List.copyOf(questions);
	}

	/**
	 * Возвращает вопрос по id.
	 *
	 * @param id идентификатор вопроса
	 * @return вопрос или null
	 */
	public Question getQuestionById(String id) {
		return questions.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
	}
}
