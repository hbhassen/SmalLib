import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface WhoAmIResponse {
  nameId: string;
  sessionIndex: string | null;
  attributes: Record<string, unknown>;
}

export interface MessageResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class WhoAmIService {
  private readonly apiUrl = 'http://localhost:8080/demo2/api/whoami';
  private readonly messageUrl = 'http://localhost:8080/demo2/api/message';

  constructor(private readonly http: HttpClient) {}

  getWhoAmI(): Observable<WhoAmIResponse | null> {
    return this.http
      .get(this.apiUrl, { responseType: 'text', withCredentials: true })
      .pipe(map((body) => this.handleResponse(body)));
  }

  getMessage(): Observable<MessageResponse> {
    return this.http.get<MessageResponse>(this.messageUrl, { withCredentials: true });
  }

  private handleResponse(body: string): WhoAmIResponse | null {
    const trimmed = (body ?? '').trim();
    if (!trimmed) {
      throw new Error('Empty response');
    }
    if (trimmed.startsWith('<')) {
      this.submitSamlForm(trimmed);
      return null;
    }
    return JSON.parse(trimmed) as WhoAmIResponse;
  }

  private submitSamlForm(html: string): void {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const sourceForm = doc.querySelector('form');
    if (!sourceForm) {
      throw new Error('SAML form not found');
    }

    const form = document.createElement('form');
    form.method = (sourceForm.getAttribute('method') ?? 'post').toLowerCase();
    const action = sourceForm.getAttribute('action');
    if (action) {
      form.action = action;
    }

    const returnUrl = this.getReturnUrl();
    const inputs = sourceForm.querySelectorAll('input');
    inputs.forEach((input) => {
      const cloned = document.createElement('input');
      cloned.type = input.getAttribute('type') ?? 'hidden';
      const name = input.getAttribute('name') ?? '';
      cloned.name = name;
      cloned.value = name === 'RelayState'
        ? returnUrl
        : input.getAttribute('value') ?? '';
      form.appendChild(cloned);
    });

    document.body.appendChild(form);
    form.submit();
  }

  private getReturnUrl(): string {
    return window.location.href;
  }
}
