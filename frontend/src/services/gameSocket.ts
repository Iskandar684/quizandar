import { Client } from '@stomp/stompjs';
import type { IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { AnswerRecord, Player, Question, ScoreMap } from '@/types/game';

/**
 * Сервис для работы с игровым WebSocket (STOMP).
 * Предоставляет методы подключения, подписки и отправки сообщений.
 */
class GameSocketService {
  /** STOMP-клиент */
  private client: Client | null = null;
  /** Идентификатор игрока (для персонализированных сообщений) */
  private playerId: string | null = null;

  /**
   * Подключается к WebSocket-серверу.
   * @param onConnected — колбэк, вызываемый после успешного подключения
   */
  connect(onConnected?: () => void): void {
    const socket = new SockJS('/ws');
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      debug: (msg) => console.debug('[STOMP]', msg),
      onConnect: () => {
        console.info('WebSocket connected');
        onConnected?.();
      },
      onStompError: (frame) => {
        console.error('STOMP error', frame.headers['message'], frame.body);
      }
    });
    this.client.activate();
  }

  /**
   * Устанавливает идентификатор игрока (для персонализированных очередей).
   * @param id — идентификатор игрока
   */
  setPlayerId(id: string): void {
    this.playerId = id;
  }

  /**
   * Подписывается на новые вопросы.
   * @param handler — обработчик получения вопроса
   * @returns функция отписки
   */
  onQuestion(handler: (question: Question) => void): () => void {
    return this.subscribe('/topic/game/question', (msg) => handler(JSON.parse(msg.body)));
  }

  /**
   * Подписывается на результаты текущего вопроса.
   * @param handler — обработчик получения результатов
   * @returns функция отписки
   */
  onResults(handler: (results: AnswerRecord[]) => void): () => void {
    return this.subscribe('/topic/game/results', (msg) => handler(JSON.parse(msg.body)));
  }

  /**
   * Подписывается на обновление списка игроков.
   * @param handler — обработчик получения списка игроков
   * @returns функция отписки
   */
  onPlayers(handler: (players: Player[]) => void): () => void {
    return this.subscribe('/topic/game/players', (msg) => handler(JSON.parse(msg.body)));
  }

  /**
   * Подписывается на обновление общего счёта.
   * @param handler — обработчик получения счёта
   * @returns функция отписки
   */
  onScores(handler: (scores: ScoreMap) => void): () => void {
    return this.subscribe('/topic/game/scores', (msg) => handler(JSON.parse(msg.body)));
  }

  /**
   * Подписывается на персональный результат ответа.
   * @param handler — обработчик получения результата
   * @returns функция отписки
   */
  onPersonalAnswerResult(handler: (record: AnswerRecord) => void): () => void {
    if (!this.playerId) {
      console.warn('playerId не установлен');
      return () => {};
    }
    return this.subscribe(`/user/${this.playerId}/queue/answer-result`, (msg) => handler(JSON.parse(msg.body)));
  }

  /**
   * Отправляет ответ игрока.
   * @param optionId — выбранный вариант ответа
   */
  sendAnswer(optionId: string): void {
    if (!this.playerId) {
      console.error('playerId не установлен');
      return;
    }
    this.publish('/app/game/main/answer', {
      playerId: this.playerId,
      optionId
    });
  }

  /**
   * Отправляет команду ведущего «Начать игру».
   */
  startGame(): void {
    this.publish('/app/game/main/start', {});
  }

  /**
   * Отправляет команду ведущего «Следующий вопрос».
   */
  nextQuestion(): void {
    this.publish('/app/game/main/next', {});
  }

  /**
   * Утилита подписки на топик.
   * @param destination — адрес топика
   * @param handler — обработчик сообщения
   * @returns функция отписки
   */
  private subscribe(destination: string, handler: (msg: IMessage) => void): () => void {
    if (!this.client || !this.client.connected) {
      console.warn('STOMP-клиент не подключён');
      return () => {};
    }
    const sub = this.client.subscribe(destination, handler);
    return () => sub.unsubscribe();
  }

  /**
   * Утилита публикации сообщения.
   * @param destination — адрес назначения
   * @param body — объект для отправки
   */
  private publish(destination: string, body: unknown): void {
    if (!this.client || !this.client.connected) {
      console.warn('STOMP-клиент не подключён');
      return;
    }
    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });
  }
}

// Экспортируем singleton
export const gameSocket = new GameSocketService();