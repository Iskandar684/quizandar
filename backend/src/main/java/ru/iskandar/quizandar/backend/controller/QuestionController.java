package ru.iskandar.quizandar.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.iskandar.quizandar.backend.model.Question;
import ru.iskandar.quizandar.backend.service.QuestionService;

import java.util.List;

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
}
