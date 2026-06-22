import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ConsultaRequest, ConsultaResponse } from '../models/consulta.model';

@Injectable({
  providedIn: 'root',
})
export class ConsultaService {
  private readonly apiUrl = 'http://localhost:8080/api/consulta';

  constructor(private readonly http: HttpClient) {}

  consultar(problema: string): Observable<ConsultaResponse> {
    const body: ConsultaRequest = { problema };
    return this.http.post<ConsultaResponse>(this.apiUrl, body);
  }
}
