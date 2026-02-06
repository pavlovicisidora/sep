import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PaymentFormData {
  paymentId: string;
  amount: number;
  currency: string;
  expired: boolean;
}

export interface ProcessPaymentRequest {
  paymentId: string;
  pan: string;
  cardHolderName: string;
  expiryDate: string;
  securityCode: string;
}

export interface ProcessPaymentResponse {
  globalTransactionId: string;
  stan: string;
  status: string;
  message: string;
  redirectUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private apiUrl = 'https://localhost:8445/api/payment';

  constructor(private http: HttpClient) {}

  getPaymentFormData(paymentId: string): Observable<PaymentFormData> {
    return this.http.get<PaymentFormData>(`${this.apiUrl}/form/${paymentId}`);
  }

  processPayment(request: ProcessPaymentRequest): Observable<ProcessPaymentResponse> {
    return this.http.post<ProcessPaymentResponse>(`${this.apiUrl}/process`, request);
  }
}
