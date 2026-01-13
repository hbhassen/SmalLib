import { Component, OnInit, signal } from '@angular/core';
import { WhoAmIService } from './whoami.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly message = signal('Chargement...');
  protected readonly error = signal<string | null>(null);
  protected readonly apiMessage = signal<string | null>(null);
  protected readonly apiError = signal<string | null>(null);

  constructor(private readonly whoAmIService: WhoAmIService) {}

  ngOnInit(): void {
    this.whoAmIService.getWhoAmI().subscribe({
      next: (response) => {
        if (!response) {
          this.message.set('Redirection vers IdP...');
          this.error.set(null);
          return;
        }
        const nameId = response?.nameId ?? '';
        this.message.set(`bienvenu ${nameId}`);
        this.error.set(null);
      },
      error: () => {
        this.message.set('bienvenu');
        this.error.set('Impossible de charger le profil.');
      }
    });
  }

  onFetchMessage(): void {
    this.apiError.set(null);
    this.apiMessage.set(null);
    this.whoAmIService.getMessage().subscribe({
      next: (response) => {
        this.apiMessage.set(response?.message ?? '');
      },
      error: () => {
        this.apiError.set('Erreur lors de l appel API.');
      }
    });
  }
}
