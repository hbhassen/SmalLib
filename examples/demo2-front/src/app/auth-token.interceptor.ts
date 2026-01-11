import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthTokenService } from './auth-token.service';

@Injectable()
export class AuthTokenInterceptor implements HttpInterceptor {
  constructor(private readonly authTokenService: AuthTokenService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.authTokenService.getToken();
    const authRequest = token
      ? request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        })
      : request;

    return next.handle(authRequest).pipe(
      tap((event) => {
        if (event instanceof HttpResponse) {
          const headerToken = event.headers.get('X-Auth-Token');
          if (headerToken) {
            this.authTokenService.setToken(headerToken);
          }
        }
      })
    );
  }
}
