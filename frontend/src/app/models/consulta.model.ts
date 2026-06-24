export interface ConsultaRequest {
  problema: string;
}

export interface ConsultaResponse {
  resposta: string;
  consultaId: number;
}

export interface FeedbackRequest {
  util: boolean;
}
