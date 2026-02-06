import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService, PaymentFormData } from '../../services/payment.service';
import { LuhnValidator } from '../../utils/luhn-validator';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.css']
})
export class PaymentFormComponent implements OnInit {
  paymentForm: FormGroup;
  paymentData: PaymentFormData | null = null;
  paymentId: string = '';
  loading = true;
  processing = false;
  error: string | null = null;
  cardType: 'visa' | 'mastercard' | 'amex' | 'dinners' | 'unknown' = 'unknown';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {
    this.paymentForm = this.fb.group({
      pan: ['', [Validators.required, this.panValidator.bind(this)]],
      cardHolderName: ['', [Validators.required, Validators.minLength(3)]],
      expiryDate: ['', [Validators.required, this.expiryDateValidator.bind(this)]],
      securityCode: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]]
    });

    this.paymentForm.get('pan')?.valueChanges.subscribe(pan => {
      this.cardType = LuhnValidator.detectCardType(pan?.replace(/\s/g, '') || '');
    });
  }

  ngOnInit(): void {
    this.paymentId = this.route.snapshot.paramMap.get('paymentId') || '';
    this.loadPaymentData();
  }

  loadPaymentData(): void {
    this.paymentService.getPaymentFormData(this.paymentId).subscribe({
      next: (data) => {
        this.paymentData = data;
        this.loading = false;

        if (data.expired) {
          this.error = 'Payment session has expired';
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Payment not found or has expired';
        console.error('Error loading payment data:', err);
      }
    });
  }

  panValidator(control: any) {
    const pan = control.value?.replace(/\s/g, '');
    if (!pan) return null;

    return LuhnValidator.validatePan(pan) ? null : { invalidPan: true };
  }

  expiryDateValidator(control: any) {
    const value = control.value;
    if (!value) return null;

    // Format: MM/YY
    if (!/^\d{2}\/\d{2}$/.test(value)) {
      return { invalidFormat: true };
    }

    const [month, year] = value.split('/').map((v: string) => parseInt(v, 10));

    if (month < 1 || month > 12) {
      return { invalidMonth: true };
    }

    const now = new Date();
    const currentYear = now.getFullYear() % 100; // YY format
    const currentMonth = now.getMonth() + 1;

    if (year < currentYear || (year === currentYear && month < currentMonth)) {
      return { expired: true };
    }

    return null;
  }

  formatPan(event: any): void {
    let value = event.target.value.replace(/\s/g, '');
    if (value.length > 19) {
      value = value.substring(0, 19);
    }
    event.target.value = LuhnValidator.formatPan(value);
  }

  formatExpiryDate(event: any): void {
    let value = event.target.value.replace(/\D/g, '');
    if (value.length >= 2) {
      value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    event.target.value = value;
  }

  onSubmit(): void {
    if (this.paymentForm.invalid || !this.paymentData || this.paymentData.expired) {
      this.paymentForm.markAllAsTouched();
      return;
    }

    this.processing = true;
    this.error = null;

    const formValue = this.paymentForm.value;
    const request = {
      paymentId: this.paymentId,
      pan: formValue.pan.replace(/\s/g, ''),
      cardHolderName: formValue.cardHolderName,
      expiryDate: formValue.expiryDate,
      securityCode: formValue.securityCode
    };

    this.paymentService.processPayment(request).subscribe({
      next: (response) => {
        if (response.status === 'SUCCESS' && response.redirectUrl) {
          // Redirect to WebShop via URL provided by PSP chain
          setTimeout(() => {
            window.location.href = response.redirectUrl;
          }, 1500);
          alert('Payment successful! Redirecting to store...');
        } else {
          this.error = response.message;
        }
        this.processing = false;
      },
      error: (err) => {
        // On failure, use redirectUrl from error response if available
        const redirectUrl = err.error?.redirectUrl;
        if (redirectUrl) {
          this.error = err.error?.message || 'Payment failed';
          setTimeout(() => {
            window.location.href = redirectUrl;
          }, 2000);
        } else {
          this.error = err.error?.message || 'Payment processing error';
        }
        this.processing = false;
        console.error('Payment error:', err);
      }
    });
  }

  get f() {
    return this.paymentForm.controls;
  }
}
