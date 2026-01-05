import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateOrderRequest {
  vehicleId: number;
  rentalStartDate: string;
  rentalEndDate: string;
}

export interface Order {
  id: number;
  userId: number;
  vehicleId: number;
  vehicleName: string;
  rentalStartDate: string;
  rentalEndDate: string;
  totalPrice: number;
  currency: string;
  status: string;
  merchantOrderId: string;
  createdAt: string;
}

export interface PaymentInitResponse {
  paymentId: string;
  paymentUrl: string;
  stan: string;
  status: string;
  message: string;
}

export interface OrderStatus {
  orderId: number;
  merchantOrderId: string;
  status: string;
  amount: number;
  currency: string;
  globalTransactionId: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private apiUrl = 'https://localhost:8443/api/orders';

  constructor(private http: HttpClient) {}

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, request);
  }

  getMyOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/my`);
  }

  initiatePayment(orderId: number): Observable<PaymentInitResponse> {
    return this.http.post<PaymentInitResponse>(`${this.apiUrl}/${orderId}/pay`, {});
  }

  checkOrderStatus(orderId: number): Observable<OrderStatus> {
    return this.http.get<OrderStatus>(`${this.apiUrl}/${orderId}/status`);
  }
}
