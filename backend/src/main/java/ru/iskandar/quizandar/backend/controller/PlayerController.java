package ru.iskandar.quizandar.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import ru.iskandar.quizandar.backend.request.CreatePlayerRequest;
import ru.iskandar.quizandar.backend.response.PlayerResponse;
import ru.iskandar.quizandar.backend.service.PlayerService;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

	private final PlayerService playerService;

	@Operation(summary = "Регистрация игрока", description = "Создаёт нового игрока и возвращает его описание")
	@ApiResponse(responseCode = "200", description = "Игрок успешно зарегистрирован")
	@ApiResponse(responseCode = "400", description = "Некорректное имя")
	@PostMapping("/register")
	public ResponseEntity<PlayerResponse> register(@RequestBody CreatePlayerRequest request) {
		if (request.getName() == null || request.getName().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.ok(playerService.register(request));
	}

	/**
	 * Получить игрока по ID. GET /api/players/{id}
	 */
	@GetMapping("/{id}")
	public ResponseEntity<PlayerResponse> getPlayer(@PathVariable String id) {
		PlayerResponse player = playerService.getById(id);
		if (player == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(player);
	}

	/**
	 * Список всех игроков (для отладки). GET /api/players
	 */
	@GetMapping
	public ResponseEntity<Map<String, PlayerResponse>> getAll() {
		return ResponseEntity.ok(playerService.getAll());
	}
}
