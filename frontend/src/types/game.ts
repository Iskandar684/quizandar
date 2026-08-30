/**
 * Тип вопроса: одиночный выбор, множественный выбор, истина/ложь.
 */
export type QuestionType = 'SINGLE' | 'TRUE_FALSE';

/**
 * Вариант ответа.
 */
export interface Option {
  /** Идентификатор варианта */
  id: string;
  /** Текст варианта */
  text: string;
}

/**
 * Вопрос, отправляемый клиентам (без правильных ответов).
 */
export interface Question {
  /** Идентификатор вопроса */
  id: string;
  /** Текст вопроса */
  text: string;
  /** Тип вопроса (одиночный/множественный) */
  type: QuestionType;
  /** Лимит времени в секундах (0 — без лимита) */
  timeLimitSec: number;
  /** Список вариантов */
  options: Option[];
}

/**
 * Запись об ответе игрока (результат).
 */
export interface AnswerRecord {
  /** Идентификатор игрока */
  playerId: string;
  /** Выбранный вариант */
  selectedOptionId: string;
  /** Время, затраченное на ответ (мс) */
  timeTakenMs: number;
  /** Правильный ли ответ */
  correct: boolean;
  /** Начисленные баллы */
  pointsAwarded: number;
}

/**
 * Информация об игроке.
 */
export interface Player {
  /** Идентификатор игрока */
  id: string;
  /** Имя игрока */
  name: string;
}

/**
 * Текущий счёт игроков (ключ — playerId).
 */
export type ScoreMap = Record<string, number>;