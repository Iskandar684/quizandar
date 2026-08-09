package ru.iskandar.quizandar.backend.request;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 
 */
@Data
@Accessors(prefix = "_")
public class CreatePlayerRequest {

	private String _name;
}