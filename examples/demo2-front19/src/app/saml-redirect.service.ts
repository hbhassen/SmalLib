import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SamlRedirectService {
  handleHtmlResponse(body: string | null | undefined): boolean {
    const trimmed = (body ?? '').trim();
    if (!trimmed) {
      return false;
    }
    const lower = trimmed.toLowerCase();
    if (!lower.startsWith('<') || !lower.includes('<form') || !lower.includes('samlrequest')) {
      return false;
    }
    return this.submitSamlForm(trimmed);
  }

  private submitSamlForm(html: string): boolean {
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    const sourceForm = doc.querySelector('form');
    if (!sourceForm) {
      return false;
    }

    const inputs = Array.from(sourceForm.querySelectorAll('input'));
    const hasSamlRequest = inputs.some(
      (input) => (input.getAttribute('name') ?? '').toLowerCase() === 'samlrequest'
    );
    if (!hasSamlRequest) {
      return false;
    }

    const form = document.createElement('form');
    form.method = (sourceForm.getAttribute('method') ?? 'post').toLowerCase();
    const action = sourceForm.getAttribute('action');
    if (action) {
      form.action = action;
    }

    const returnUrl = this.getReturnUrl();
    inputs.forEach((input) => {
      const cloned = document.createElement('input');
      cloned.type = input.getAttribute('type') ?? 'hidden';
      const name = input.getAttribute('name') ?? '';
      cloned.name = name;
      cloned.value = name.toLowerCase() === 'relaystate'
        ? returnUrl
        : input.getAttribute('value') ?? '';
      form.appendChild(cloned);
    });

    document.body.appendChild(form);
    form.submit();
    return true;
  }

  private getReturnUrl(): string {
    return window.location.href;
  }
}
