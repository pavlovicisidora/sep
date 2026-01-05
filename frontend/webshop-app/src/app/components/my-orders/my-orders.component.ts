import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OrderService, Order } from '../../services/order.service';
import { interval, Subscription, switchMap } from 'rxjs';

@Component({
  selector: 'app-my-orders',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-orders.component.html',
  styleUrls: ['./my-orders.component.css']
})
export class MyOrdersComponent implements OnInit, OnDestroy {
  orders: Order[] = [];
  loading = true;
  currentUser = this.authService.getCurrentUser();
  private pollingSubscription?: Subscription;

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
    this.startPollingForProcessingOrders();
  }

  ngOnDestroy(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
    }
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

  startPollingForProcessingOrders(): void {
    this.pollingSubscription = interval(10000)
      .pipe(
        switchMap(() => this.orderService.getMyOrders())
      )
      .subscribe({
        next: (data) => {
          const processingOrders = data.filter(o => o.status === 'PROCESSING');
          
          if (processingOrders.length > 0) {
            console.log(`Polling: ${processingOrders.length} orders in PROCESSING status`);
            
            processingOrders.forEach(order => {
              this.checkOrderStatus(order.id);
            });
          } else {
            console.log('No processing orders, stopping poll');
          }
          
          this.orders = data.sort((a, b) => 
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
        },
        error: (err) => {
          console.error('Error polling orders', err);
        }
      });
  }

  checkOrderStatus(orderId: number): void {
    this.orderService.checkOrderStatus(orderId).subscribe({
      next: (status) => {
        console.log(`Order ${orderId} status:`, status);
        
        const currentOrder = this.orders.find(o => o.id === orderId);
        if (currentOrder && currentOrder.status !== status.status) {
          console.log(`Status changed from ${currentOrder.status} to ${status.status}`);
          this.loadOrders();
        }
      },
      error: (err) => {
        console.error(`Error checking status for order ${orderId}`, err);
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
