import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { VehicleService, Vehicle } from '../../services/vehicle.service';
import { OrderService } from '../../services/order.service';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { PaymentMethod, PaymentMethodService } from 'src/app/services/payment-method.service';

@Component({
  selector: 'app-vehicle-catalog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './vehicle-catalog.component.html',
  styleUrls: ['./vehicle-catalog.component.css']
})
export class VehicleCatalogComponent implements OnInit {
  vehicles: Vehicle[] = [];
  loading = true;
  currentUser = this.authService.getCurrentUser();

  selectedVehicle: Vehicle | null = null;
  showBookingModal = false;
  bookingForm: FormGroup;
  processingOrder = false;
  error: string | null = null;

  selectedPaymentMethod: string | null = null;
  showPaymentMethodSelection = false;
  
  availablePaymentMethods: PaymentMethod[] = [];

  constructor(
    private vehicleService: VehicleService,
    private orderService: OrderService,
    private authService: AuthService,
    private router: Router,
    private fb: FormBuilder,
    private paymentMethodService: PaymentMethodService
  ) {
    this.bookingForm = this.fb.group({
      rentalStartDate: ['', Validators.required],
      rentalEndDate: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadVehicles();
    this.loadPaymentMethods();
  }

  loadPaymentMethods(): void {
    this.paymentMethodService.getAvailablePaymentMethods().subscribe({
      next: (methods) => {
        this.availablePaymentMethods = methods;
        console.log('Available payment methods:', methods);
      },
      error: (err) => {
        console.error('Error loading payment methods', err);
      }
    });
  }

  loadVehicles(): void {
    this.vehicleService.getAvailableVehicles().subscribe({
      next: (data) => {
        this.vehicles = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading vehicles', err);
        this.loading = false;
      }
    });
  }

  openBookingModal(vehicle: Vehicle): void {
    this.selectedVehicle = vehicle;
    this.showBookingModal = true;
    this.error = null;
    
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const endDate = new Date();
    endDate.setDate(endDate.getDate() + 5);
    
    this.bookingForm.patchValue({
      rentalStartDate: tomorrow.toISOString().split('T')[0],
      rentalEndDate: endDate.toISOString().split('T')[0]
    });
  }

  closeBookingModal(): void {
    this.showBookingModal = false;
    this.selectedVehicle = null;
    this.bookingForm.reset();
  }

  calculateTotalPrice(): number {
    if (!this.selectedVehicle || !this.bookingForm.valid) return 0;
    
    const start = new Date(this.bookingForm.value.rentalStartDate);
    const end = new Date(this.bookingForm.value.rentalEndDate);
    const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    
    return days * this.selectedVehicle.pricePerDay;
  }

  onBookVehicle(): void {
    if (this.bookingForm.invalid || !this.selectedVehicle) {
      this.bookingForm.markAllAsTouched();
      return;
    }
    this.showPaymentMethodSelection = true;
  }

  selectPaymentMethod(methodCode: string): void {
    this.selectedPaymentMethod = methodCode;
    this.proceedWithPayment();
  }

  proceedWithPayment(): void {
    if (!this.selectedVehicle || !this.selectedPaymentMethod) return;
    
    this.processingOrder = true;
    this.error = null;

    const request = {
      vehicleId: this.selectedVehicle.id,
      rentalStartDate: this.bookingForm.value.rentalStartDate,
      rentalEndDate: this.bookingForm.value.rentalEndDate
    };

    this.orderService.createOrder(request).subscribe({
      next: (order) => {
        this.orderService.initiatePayment(order.id, this.selectedPaymentMethod!).subscribe({
          next: (paymentResponse) => {
            window.location.href = paymentResponse.paymentUrl;
          },
          error: (err) => {
            this.error = 'Failed to initialize payment. Please try again.';
            this.processingOrder = false;
            this.showPaymentMethodSelection = false;
          }
        });
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to create order. Please try again.';
        this.processingOrder = false;
        this.showPaymentMethodSelection = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToMyOrders(): void {
    this.router.navigate(['/my-orders']);
  }
  
  getTomorrowDate(): string {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow.toISOString().split('T')[0];
}
}
