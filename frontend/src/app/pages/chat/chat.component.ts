import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { ConsultaService } from '../../services/consulta.service';

interface ChatMessage {
  problema: string;
  resposta?: string;
  loading: boolean;
  error?: string;
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
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss',
})
export class ChatComponent {
  private readonly consultaService = inject(ConsultaService);
  private readonly destroyRef = inject(DestroyRef);

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

    const message: ChatMessage = { problema, loading: true };
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
          message.loading = false;
        },
        error: () => {
          message.error =
            'Não foi possível obter uma resposta. Verifique se o backend está rodando e tente novamente.';
          message.loading = false;
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
