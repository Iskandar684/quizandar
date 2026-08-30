package ru.iskandar.quizandar.backend.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Запрос на подключение существующего игрока к игре. Используется, когда игрок
 * уже зарегистрирован ранее и хочет восстановить сессию после перезагрузки
 * страницы или сервера.
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Accessors(prefix = "_")
public class JoinPlayerRequest {

	/** Идентификатор игрока, сохранённый на клиенте. */
	@NonNull
	private String _id;

	/** Имя игрока. */
	@NonNull
	private String _name;
}
