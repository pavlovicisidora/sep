import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface QrPaymentData {
  paymentId: string;
  amount: number;
  currency: string;
  recipientName: string;
  qrCodeBase64: string;
  expiresAt: string;
  stan: string;
  status?: string;
}

@Component({
  selector: 'app-qr-payment',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './qr-payment.component.html',
  styleUrls: ['./qr-payment.component.css']
})
export class QrPaymentComponent implements OnInit, OnDestroy {
  paymentData: QrPaymentData | null = null;
  loading = true;
  error: string | null = null;
  processing = false;
  accountNumber = '';
  accountError: string | null = null;

  timeRemaining = 0;
  private intervalId?: number;

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.processing) {
      event.preventDefault();
    }
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const paymentId = this.route.snapshot.paramMap.get('paymentId');

    if (paymentId) {
      this.loadQrPaymentData(paymentId);
    } else {
      this.error = 'Invalid payment ID';
      this.loading = false;
    }
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  loadQrPaymentData(paymentId: string): void {
    this.http.get<QrPaymentData>(`https://localhost:8445/api/qr/${paymentId}`)
      .subscribe({
        next: (data) => {
          this.paymentData = data;
          this.loading = false;
          this.startCountdown();
        },
        error: (err) => {
          console.error('Error loading QR payment data', err);
          this.error = 'Failed to load payment information';
          this.loading = false;
        }
      });
  }

  startCountdown(): void {
    this.updateTimeRemaining();

    this.intervalId = window.setInterval(() => {
      this.updateTimeRemaining();
    }, 1000);
  }

  updateTimeRemaining(): void {
    if (!this.paymentData) return;

    const expiryTime = new Date(this.paymentData.expiresAt).getTime();
    const now = Date.now();
    this.timeRemaining = Math.max(0, Math.floor((expiryTime - now) / 1000));
  }

  formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  get qrCodeImage(): string {
    if (!this.paymentData) return '';
    return `data:image/png;base64,${this.paymentData.qrCodeBase64}`;
  }

  get amount(): number {
    return this.paymentData?.amount || 0;
  }

  get merchantName(): string {
    return this.paymentData?.recipientName || '';
  }

  get merchantAccount(): string {
    return 'Merchant Account';
  }

  confirmPayment(): void {
    if (!this.paymentData || !this.accountNumber.trim()) {
      this.accountError = 'Please enter your bank account number';
      return;
    }

    this.processing = true;
    this.accountError = null;
    const transactionId = parseInt(this.paymentData.paymentId.replace('QR-', ''));

    this.http.post<{status: string, redirectUrl: string, message: string, error?: string}>(
      'https://localhost:8445/api/qr/confirm',
      {
        transactionId: transactionId,
        accountNumber: this.accountNumber.trim()
      }
    ).subscribe({
      next: (response) => {
        if (response.redirectUrl) {
          setTimeout(() => {
            window.location.href = response.redirectUrl;
          }, 1000);
        }
      },
      error: (err) => {
        console.error('Confirmation failed:', err);
        this.processing = false;
        const errorMsg = err.error?.error || 'Payment confirmation failed';
        this.accountError = errorMsg;

        // If there's a redirect URL in the error, use it after a delay
        if (err.error?.redirectUrl) {
          setTimeout(() => {
            window.location.href = err.error.redirectUrl;
          }, 2000);
        }
      }
    });
  }

  openScanner(): void {
    if (this.paymentData) {
      this.router.navigate(['/qr-scanner', this.paymentData.paymentId]);
    }
  }

  refreshQR(): void {
    if (this.paymentData) {
      this.loading = true;
      this.loadQrPaymentData(this.paymentData.paymentId);
    }
  }
}
