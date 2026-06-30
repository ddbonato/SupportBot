import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminLoginRequest, AdminLoginResponse } from '../models/admin.model';
import { KnowledgeItem, KnowledgeRequest } from '../models/knowledge.model';

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  private readonly adminUrl = '/api/admin';

  constructor(private readonly http: HttpClient) {}

  loginKnowledge(senha: string): Observable<AdminLoginResponse> {
    const body: AdminLoginRequest = { senha };
    return this.http.post<AdminLoginResponse>(`${this.adminUrl}/knowledge/login`, body);
  }

  listarCasos(): Observable<KnowledgeItem[]> {
    return this.http.get<KnowledgeItem[]>(`${this.adminUrl}/knowledge`);
  }

  criarCaso(request: KnowledgeRequest): Observable<KnowledgeItem> {
    return this.http.post<KnowledgeItem>(`${this.adminUrl}/knowledge`, request);
  }

  atualizarCaso(indice: number, request: KnowledgeRequest): Observable<KnowledgeItem> {
    return this.http.put<KnowledgeItem>(`${this.adminUrl}/knowledge/${indice}`, request);
  }

  excluirCaso(indice: number): Observable<void> {
    return this.http.delete<void>(`${this.adminUrl}/knowledge/${indice}`);
  }
}
