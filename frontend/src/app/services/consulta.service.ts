import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ConsultaRequest,
  ConsultaResponse,
  FeedbackRequest,
} from '../models/consulta.model';

@Injectable({
  providedIn: 'root',
})
export class ConsultaService {
  private readonly apiUrl = '/api/consulta';

  constructor(private readonly http: HttpClient) {}

  consultar(problema: string): Observable<ConsultaResponse> {
    const body: ConsultaRequest = { problema };
    return this.http.post<ConsultaResponse>(this.apiUrl, body);
  }

  enviarFeedback(consultaId: number, util: boolean): Observable<void> {
    const body: FeedbackRequest = { util };
    return this.http.patch<void>(`${this.apiUrl}/${consultaId}/feedback`, body);
  }
}
