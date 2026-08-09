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
     * @param request запрос с именем
     * @return ответ с id и именем
     */
    public PlayerResponse register(@NonNull CreatePlayerRequest request) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        PlayerResponse player = PlayerResponse.builder()
                .id(id)
                .name(request.getName().trim())
                .build();
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
