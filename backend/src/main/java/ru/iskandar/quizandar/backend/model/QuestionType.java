package ru.iskandar.quizandar.backend.model;

public enum QuestionType {

	/** один правильный ответ */
	SINGLE,

	/** несколько правильных */
	MULTIPLE,

	/** правда/ложь (SINGLE с двумя вариантами) */
	TRUE_FALSE
}