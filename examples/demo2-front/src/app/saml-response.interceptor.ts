import { Injectable } from '@angular/core';
import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import {
  EMPTY,
  Observable,
  catchError,
  mergeMap,
  of,
  throwError
} from 'rxjs';
import { SamlRedirectService } from './saml-redirect.service';

@Injectable()
export class SamlResponseInterceptor implements HttpInterceptor {
  constructor(private readonly samlRedirectService: SamlRedirectService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.shouldHandle(request)) {
      return next.handle(request);
    }

    const textRequest = request.clone({ responseType: 'text' as 'json' });
    return next.handle(textRequest).pipe(
      mergeMap((event) => {
        if (!(event instanceof HttpResponse)) {
          return of(event);
        }

        const body = event.body;
        if (typeof body !== 'string') {
          return of(event);
        }

        const trimmed = body.trim();
        if (this.samlRedirectService.handleHtmlResponse(trimmed)) {
          return of(event.clone({ body: null }));
        }

        if (!trimmed) {
          return of(event.clone({ body: null }));
        }

        try {
          const parsed = JSON.parse(trimmed);
          return of(event.clone({ body: parsed }));
        } catch (error) {
          return throwError(() => error);
        }
      }),
      catchError((error: HttpErrorResponse) => {
        if (typeof error.error === 'string') {
          const trimmed = error.error.trim();
          if (this.samlRedirectService.handleHtmlResponse(trimmed)) {
            return of(new HttpResponse({ status: 200, body: null }));
          }
        }
        return throwError(() => error);
      })
    );
  }

  private shouldHandle(request: HttpRequest<unknown>): boolean {
    if (request.responseType !== 'json') {
      return false;
    }
    return request.url.includes('/api/');
  }
}
