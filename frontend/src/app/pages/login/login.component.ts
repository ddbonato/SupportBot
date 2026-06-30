import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { ChatAuthService } from '../../services/chat-auth.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly chatAuthService = inject(ChatAuthService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  carregando = false;
  erro: string | null = null;

  readonly loginForm = new FormGroup({
    senha: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  entrar(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.erro = null;
    this.carregando = true;

    this.chatAuthService
      .login(this.loginForm.controls.senha.value)
      .pipe(
        finalize(() => {
          this.carregando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.authService.setChatToken(response.token);
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
          this.router.navigateByUrl(returnUrl);
        },
        error: (err) => {
          if (err?.status === 401) {
            this.erro = 'Senha incorreta. Tente novamente.';
          } else if (err?.status === 0) {
            this.erro =
              'Não foi possível conectar ao backend. Verifique se o servidor está rodando na porta 8080.';
          } else {
            this.erro = `Erro ao fazer login (${err?.status ?? 'desconhecido'}).`;
          }
        },
      });
  }
}
