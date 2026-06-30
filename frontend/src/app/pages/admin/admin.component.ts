import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs';

import { KnowledgeItem } from '../../models/knowledge.model';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  knowledgeAutenticado = false;
  carregando = false;
  salvando = false;
  erro: string | null = null;

  casos: KnowledgeItem[] = [];
  casosFiltrados: KnowledgeItem[] = [];

  exibindoFormulario = false;
  editandoIndice: number | null = null;

  readonly loginForm = new FormGroup({
    senha: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  readonly buscaControl = new FormControl('', { nonNullable: true });

  readonly casoForm = new FormGroup({
    problema: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3)],
    }),
    solucao: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3)],
    }),
  });

  ngOnInit(): void {
    this.knowledgeAutenticado = this.authService.isKnowledgeLoggedIn();
    if (this.knowledgeAutenticado) {
      this.carregarCasos();
    }

    this.buscaControl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((termo) => this.aplicarFiltro(termo));
  }

  entrarKnowledge(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.erro = null;
    this.carregando = true;

    this.adminService
      .loginKnowledge(this.loginForm.controls.senha.value)
      .pipe(
        finalize(() => {
          this.carregando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.authService.setKnowledgeToken(response.token);
          this.knowledgeAutenticado = true;
          this.loginForm.reset();
          this.carregarCasos();
        },
        error: (err) => {
          if (err?.status === 401) {
            this.erro = 'Senha incorreta. Tente novamente.';
          } else {
            this.erro = 'Não foi possível autenticar na base de conhecimento.';
          }
        },
      });
  }

  sairKnowledge(): void {
    this.authService.clearKnowledgeToken();
    this.knowledgeAutenticado = false;
    this.casos = [];
    this.casosFiltrados = [];
    this.fecharFormulario();
    this.erro = null;
    this.router.navigate(['/']);
  }

  carregarCasos(): void {
    this.erro = null;
    this.carregando = true;

    this.adminService
      .listarCasos()
      .pipe(
        finalize(() => {
          this.carregando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (casos) => {
          this.casos = casos;
          this.aplicarFiltro(this.buscaControl.value);
        },
        error: () => {
          this.erro = 'Não foi possível carregar a base de conhecimento.';
          if (this.authService.isKnowledgeLoggedIn()) {
            this.authService.clearKnowledgeToken();
            this.knowledgeAutenticado = false;
          }
        },
      });
  }

  abrirNovoCaso(): void {
    this.editandoIndice = null;
    this.casoForm.reset();
    this.exibindoFormulario = true;
    this.erro = null;
  }

  abrirEdicao(caso: KnowledgeItem): void {
    this.editandoIndice = caso.indice;
    this.casoForm.setValue({
      problema: caso.problema,
      solucao: caso.solucao,
    });
    this.exibindoFormulario = true;
    this.erro = null;
  }

  fecharFormulario(): void {
    this.exibindoFormulario = false;
    this.editandoIndice = null;
    this.casoForm.reset();
  }

  salvarCaso(): void {
    if (this.casoForm.invalid) {
      this.casoForm.markAllAsTouched();
      return;
    }

    const body = this.casoForm.getRawValue();
    this.erro = null;
    this.salvando = true;

    const operacao =
      this.editandoIndice != null
        ? this.adminService.atualizarCaso(this.editandoIndice, body)
        : this.adminService.criarCaso(body);

    operacao
      .pipe(
        finalize(() => {
          this.salvando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.fecharFormulario();
          this.carregarCasos();
        },
        error: (err) => {
          this.erro =
            err?.error?.detail ??
            'Não foi possível salvar o caso. Verifique se o CSV está configurado para escrita.';
        },
      });
  }

  excluirCaso(caso: KnowledgeItem): void {
    const resumo = this.truncar(caso.problema, 80);
    const confirmado = window.confirm(
      `Excluir o caso "${resumo}"?\n\nEsta ação não pode ser desfeita.`,
    );
    if (!confirmado) {
      return;
    }

    this.erro = null;
    this.carregando = true;

    this.adminService
      .excluirCaso(caso.indice)
      .pipe(
        finalize(() => {
          this.carregando = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.carregarCasos();
        },
        error: () => {
          this.erro = 'Não foi possível excluir o caso.';
        },
      });
  }

  truncar(texto: string, limite: number): string {
    const comQuebras = texto.replace(/\\n/g, '\n');
    if (comQuebras.length <= limite) {
      return comQuebras;
    }

    let cortado = comQuebras.substring(0, limite);
    const ultimaQuebra = cortado.lastIndexOf('\n');
    if (ultimaQuebra > limite * 0.6) {
      cortado = cortado.substring(0, ultimaQuebra);
    }
    return cortado + '…';
  }

  private aplicarFiltro(termo: string): void {
    const busca = termo.trim().toLowerCase();
    if (!busca) {
      this.casosFiltrados = [...this.casos];
      return;
    }

    this.casosFiltrados = this.casos.filter(
      (caso) =>
        caso.problema.toLowerCase().includes(busca) ||
        caso.solucao.toLowerCase().includes(busca),
    );
  }
}
