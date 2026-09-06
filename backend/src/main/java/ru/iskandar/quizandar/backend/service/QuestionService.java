package ru.iskandar.quizandar.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.model.QuizQuestions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сервис для работы с вопросами.
 * Загружает вопросы из внешнего каталога (volume), поддерживает импорт,
 * выбор активного файла и получение списка файлов.
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

    /** Имя текущего выбранного файла (без пути). */
    private String currentFileName = null;

    /**
     * Инициализация: загрузка вопросов из каталога.
     * Если каталог пуст, загружаем встроенный пример.
     */
    @PostConstruct
    public void init() {
        loadQuestionsFromDirectory();
        if (questions.isEmpty()) {
            loadQuestionsFromResource("questions/questions-english-a1.json");
        }
    }

    /**
     * Загружает все JSON-файлы из каталога и объединяет вопросы.
     * (Используется для импорта/обновления, но активный набор загружается отдельно)
     */
    private void loadQuestionsFromDirectory() {
        Path dir = Paths.get(questionsDirectory);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error("Не удалось создать каталог {}", dir, e);
                return;
            }
        }

        List<Question> allQuestions = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();

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
     * Загружает вопросы из classpath (встроенный ресурс).
     *
     * @param path путь к ресурсу
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
     * Импортирует файл вопросов: сохраняет во внешний каталог и перезагружает список.
     *
     * @param file файл JSON с вопросами
     * @return объект с информацией о количестве импортированных и общем количестве вопросов
     * @throws IOException если произошла ошибка сохранения
     */
    public Map<String, Object> importQuestions(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }

        String originalFilename = file.getOriginalFilename();
        String filename = (originalFilename != null && originalFilename.endsWith(".json"))
                ? originalFilename
                : "imported-" + System.currentTimeMillis() + ".json";

        Path dir = Paths.get(questionsDirectory);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        file.transferTo(target.toAbsolutePath().toFile());

        int importedCount;
        try (InputStream is = Files.newInputStream(target)) {
            QuizQuestions quiz = objectMapper.readValue(is, QuizQuestions.class);
            importedCount = quiz.getQuestions().size();
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw new IOException("Ошибка чтения импортированного файла: " + e.getMessage(), e);
        }

        // После импорта автоматически выбираем этот файл как активный
        selectQuestionsFile(filename);

        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", importedCount);
        result.put("totalCount", questions.size());
        result.put("currentFile", filename);
        return result;
    }

    /**
     * Возвращает список доступных файлов вопросов с заголовками.
     *
     * @return список карт с информацией о файлах
     */
    public List<Map<String, Object>> getAvailableQuestionFiles() {
        Path dir = Paths.get(questionsDirectory);
        if (!Files.exists(dir)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> filesInfo = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();

            for (Path file : files) {
                try (InputStream is = Files.newInputStream(file)) {
                    QuizQuestions quiz = objectMapper.readValue(is, QuizQuestions.class);
                    Map<String, Object> info = new HashMap<>();
                    info.put("fileName", file.getFileName().toString());
                    info.put("title", quiz.getTitle() != null ? quiz.getTitle() : "Без названия");
                    info.put("description", quiz.getDescription() != null ? quiz.getDescription() : "");
                    info.put("questionCount", quiz.getQuestions().size());
                    filesInfo.add(info);
                } catch (IOException e) {
                    log.warn("Не удалось прочитать файл {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Ошибка чтения каталога {}", dir, e);
        }
        return filesInfo;
    }

    /**
     * Выбирает файл вопросов в качестве активного и загружает его содержимое.
     *
     * @param fileName имя файла (без пути)
     * @throws IllegalArgumentException если файл не существует или не может быть прочитан
     */
    public void selectQuestionsFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Имя файла не может быть пустым");
        }

        Path dir = Paths.get(questionsDirectory);
        Path file = dir.resolve(fileName);
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("Файл не найден: " + fileName);
        }

        try (InputStream is = Files.newInputStream(file)) {
            QuizQuestions quiz = objectMapper.readValue(is, QuizQuestions.class);
            questions = new ArrayList<>(quiz.getQuestions());
            currentFileName = fileName;
            log.info("Выбран файл вопросов: {} ({} вопросов)", fileName, questions.size());
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка чтения файла: " + fileName, e);
        }
    }

    /**
     * Возвращает имя текущего выбранного файла.
     *
     * @return имя файла или null
     */
    public String getCurrentFileName() {
        return currentFileName;
    }

    /**
     * Возвращает список всех вопросов (текущего активного набора).
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
        return questions.stream()
                .filter(q -> q.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
