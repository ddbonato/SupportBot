import { Injectable } from '@angular/core';

const CHAT_TOKEN_KEY = 'chatToken';
const KNOWLEDGE_TOKEN_KEY = 'knowledgeToken';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  getChatToken(): string | null {
    return sessionStorage.getItem(CHAT_TOKEN_KEY);
  }

  setChatToken(token: string): void {
    sessionStorage.setItem(CHAT_TOKEN_KEY, token);
  }

  clearChatToken(): void {
    sessionStorage.removeItem(CHAT_TOKEN_KEY);
  }

  isChatLoggedIn(): boolean {
    return !!this.getChatToken();
  }

  getKnowledgeToken(): string | null {
    return sessionStorage.getItem(KNOWLEDGE_TOKEN_KEY);
  }

  setKnowledgeToken(token: string): void {
    sessionStorage.setItem(KNOWLEDGE_TOKEN_KEY, token);
  }

  clearKnowledgeToken(): void {
    sessionStorage.removeItem(KNOWLEDGE_TOKEN_KEY);
  }

  isKnowledgeLoggedIn(): boolean {
    return !!this.getKnowledgeToken();
  }

  clearAll(): void {
    this.clearChatToken();
    this.clearKnowledgeToken();
  }
}
