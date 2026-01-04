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

  getStatusClass(status: string): string {
    switch (status) {
      case 'PAID': return 'status-paid';
      case 'PENDING': return 'status-pending';
      case 'PROCESSING': return 'status-processing';
      case 'FAILED': return 'status-failed';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  canPay(order: Order): boolean {
    return order.status === 'PENDING';
  }

  payOrder(orderId: number): void {
    this.orderService.initiatePayment(orderId).subscribe({
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
}
