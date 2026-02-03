import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PaymentMethod {
  code: string;
  name: string;
  description: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentMethodService {
  private pspApiUrl = 'https://localhost:8444/api/payment';

  constructor(private http: HttpClient) {}

  getAvailablePaymentMethods(): Observable<PaymentMethod[]> {
    return this.http.get<PaymentMethod[]>(`${this.pspApiUrl}/methods`);
  }
}
