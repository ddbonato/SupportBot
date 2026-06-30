import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isChatLogin = req.url.startsWith('/api/admin/login');
  const isKnowledgeLogin = req.url.startsWith('/api/admin/knowledge/login');
  const isPublic = isChatLogin || isKnowledgeLogin;

  let token: string | null = null;
  if (req.url.startsWith('/api/consulta')) {
    token = authService.getChatToken();
  } else if (req.url.startsWith('/api/admin/knowledge')) {
    token = authService.getKnowledgeToken();
  }

  if (token && req.url.startsWith('/api/') && !isPublic) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(req).pipe(
    catchError((err) => {
      if (err.status !== 401 || isPublic) {
        return throwError(() => err);
      }

      if (req.url.startsWith('/api/consulta')) {
        authService.clearChatToken();
        router.navigate(['/login'], {
          queryParams: { returnUrl: router.url },
        });
      } else if (req.url.startsWith('/api/admin/knowledge')) {
        authService.clearKnowledgeToken();
        if (!router.url.startsWith('/admin')) {
          router.navigate(['/admin']);
        }
      }

      return throwError(() => err);
    }),
  );
};
