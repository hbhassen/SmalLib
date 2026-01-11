import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
    return this.http.get<WhoAmIResponse | null>(this.apiUrl, { withCredentials: true });
  }

  getMessage(): Observable<MessageResponse | null> {
    return this.http.get<MessageResponse | null>(this.messageUrl, { withCredentials: true });
  }
}
