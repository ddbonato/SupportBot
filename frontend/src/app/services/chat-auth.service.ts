import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AdminLoginRequest, AdminLoginResponse } from '../models/admin.model';

@Injectable({
  providedIn: 'root',
})
export class ChatAuthService {
  private readonly loginUrl = '/api/admin/login';

  constructor(private readonly http: HttpClient) {}

  login(senha: string): Observable<AdminLoginResponse> {
    const body: AdminLoginRequest = { senha };
    return this.http.post<AdminLoginResponse>(this.loginUrl, body);
  }
}
