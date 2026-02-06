import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OrderService, Order } from '../../services/order.service';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.css']
})
export class MyOrdersComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  checkingStatus: { [orderId: number]: boolean } = {}; 
  currentUser = this.authService.getCurrentUser();

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.orderService.getMyOrders().subscribe({
      next: (data) => {
        this.orders = data.sort((a, b) => 
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading orders', err);
        this.loading = false;
      }
    });
  }

  checkPaymentStatusWithPSP(orderId: number): void {
    this.checkingStatus[orderId] = true;
    
    this.orderService.checkPaymentStatusWithPSP(orderId).subscribe({
      next: (status) => {
        console.log('Payment status checked with PSP:', status);
        
        this.loadOrders();
        
        this.checkingStatus[orderId] = false;
        
        if (status.status === 'SUCCESS') {
          alert('Payment confirmed! Your order is now PAID.');
        } else if (status.status === 'FAILED') {
          alert('Payment failed. Please try again.');
        } else {
          alert('Payment is still pending. Please wait or try paying again.');
        }
      },
      error: (err) => {
        console.error('Error checking payment status with PSP', err);
        this.checkingStatus[orderId] = false;
        alert('Failed to check payment status. Please try again later.');
      }
    });
  }

  canCheckStatus(order: Order): boolean {
    if (order.status !== 'PENDING') return false;
    
    const attemptTime = order.lastPaymentAttempt || order.createdAt;
    const age = Date.now() - new Date(attemptTime).getTime();
    const thirtyMinutes = 30 * 60 * 1000;
    
    return age < thirtyMinutes;
  }

  isCheckingStatus(orderId: number): boolean {
    return this.checkingStatus[orderId] || false;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PAID': return 'status-paid';
      case 'PENDING': return 'status-pending';
      case 'FAILED': return 'status-failed';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  canPay(order: Order): boolean {
    return order.status === 'PENDING';
  }

  payOrder(order: Order): void {
    const paymentMethod = order.paymentMethod || 'CARD';
  
    this.orderService.initiatePayment(order.id, paymentMethod).subscribe({
      next: (response) => {
        window.location.href = response.paymentUrl;
      },
      error: (err) => {
        console.error('Payment initiation failed', err);
        alert('Failed to initiate payment');
      }
    });
  }

  goToVehicles(): void {
    this.router.navigate(['/vehicles']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getPaymentMethodName(method: string | undefined): string {
    if (!method) return 'N/A';
    
    switch (method) {
      case 'CARD': return 'Card Payment';
      case 'QR': return 'QR Code';
      case 'PAYPAL': return 'PayPal';
      case 'CRYPTO': return 'Cryptocurrency';
      default: return method;
    }
  }
}
