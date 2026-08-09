package ru.iskandar.quizandar.backend.response;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(prefix = "_")
public class PlayerResponse {

	@NonNull
	private final String _id;

	@NonNull
	private final String _name;
}
