package ru.iskandar.quizandar.backend.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.NonNull;
import ru.iskandar.quizandar.backend.request.CreatePlayerRequest;
import ru.iskandar.quizandar.backend.response.PlayerResponse;

@Service
public class PlayerService {

	private final Map<String, PlayerResponse> players = new ConcurrentHashMap<>();

	/**
	 * Регистрирует нового игрока.
	 * 
	 * @param request запрос с именем
	 * @return ответ с id и именем
	 */
	public PlayerResponse register(@NonNull CreatePlayerRequest request) {
		String id = UUID.randomUUID().toString().substring(0, 8);
		PlayerResponse player = PlayerResponse.builder().id(id).name(request.getName().trim()).build();
		players.put(id, player);
		return player;
	}

	/**
	 * Регистрирует игрока с указанным идентификатором (восстановление сессии). Если
	 * игрок с таким ID уже есть, его данные будут перезаписаны новым именем.
	 *
	 * @param id   идентификатор игрока
	 * @param name имя игрока
	 * @return объект игрока
	 */
	public PlayerResponse registerWithId(@NonNull String id, @NonNull String name) {
		PlayerResponse player = PlayerResponse.builder().id(id).name(name.trim()).build();
		players.put(id, player);
		return player;
	}

	/**
	 * Возвращает игрока по идентификатору.
	 */
	public PlayerResponse getById(@NonNull String id) {
		return players.get(id);
	}

	/**
	 * Возвращает копию списка всех игроков.
	 */
	public Map<String, PlayerResponse> getAll() {
		return Map.copyOf(players);
	}
}
