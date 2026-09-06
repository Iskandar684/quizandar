package ru.iskandar.quizandar.backend.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.service.QuestionService;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

	private final QuestionService questionService;

	@GetMapping
	public ResponseEntity<List<Question>> getAllQuestions() {
		return ResponseEntity.ok(questionService.getAllQuestions());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Question> getQuestion(@PathVariable String id) {
		Question q = questionService.getQuestionById(id);
		if (q == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(q);
	}

	/**
	 * Импортирует вопросы из загруженного JSON-файла.
	 *
	 * @param file файл с вопросами
	 * @return сообщение о результате
	 */
	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file) {
		try {
			int count = questionService.importQuestions(file);
			return ResponseEntity.ok(Map.of("status", "success", "importedCount", count));
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("status", "error", "message", "Ошибка сохранения файла: " + e.getMessage()));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
		}
	}
}
