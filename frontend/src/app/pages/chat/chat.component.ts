import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MarkdownComponent } from 'ngx-markdown';
import { finalize } from 'rxjs';

import { ConsultaService } from '../../services/consulta.service';

interface ChatMessage {
  id: number;
  problema: string;
  resposta?: string;
  consultaId?: number;
  loading: boolean;
  error?: string;
  feedback?: boolean;
  feedbackEnviando?: boolean;
  feedbackErro?: string;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MarkdownComponent,
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent {
  private readonly consultaService = inject(ConsultaService);
  private readonly destroyRef = inject(DestroyRef);

  private nextMessageId = 0;

  readonly problemaControl = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.minLength(3)],
  });

  messages: ChatMessage[] = [];
  enviando = false;

  get hasMessages(): boolean {
    return this.messages.length > 0;
  }

  enviar(): void {
    if (this.problemaControl.invalid || this.enviando) {
      this.problemaControl.markAsTouched();
      return;
    }

    const problema = this.problemaControl.value.trim();
    if (!problema) {
      return;
    }

    const message: ChatMessage = {
      id: ++this.nextMessageId,
      problema,
      loading: true,
    };
    this.messages = [...this.messages, message];
    this.problemaControl.reset();
    this.enviando = true;

    this.consultaService
      .consultar(problema)
      .pipe(
        finalize(() => {
          this.enviando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          message.resposta = response.resposta;
          message.consultaId = response.consultaId;
          message.loading = false;
        },
        error: () => {
          message.error =
            'Não foi possível obter uma resposta. Verifique se o backend está rodando e tente novamente.';
          message.loading = false;
        },
      });
  }

  enviarFeedback(message: ChatMessage, util: boolean): void {
    if (
      message.consultaId == null ||
      message.feedback != null ||
      message.feedbackEnviando
    ) {
      return;
    }

    message.feedbackEnviando = true;
    message.feedbackErro = undefined;

    this.consultaService
      .enviarFeedback(message.consultaId, util)
      .pipe(
        finalize(() => {
          message.feedbackEnviando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          message.feedback = util;
        },
        error: () => {
          message.feedbackErro =
            'Não foi possível registrar o feedback. Tente novamente.';
        },
      });
  }

  onEnter(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (!keyboardEvent.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }
}
