import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
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
}

@Component({
  selector: 'app-qr-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './qr-payment.component.html',
  styleUrls: ['./qr-payment.component.css']
})
export class QrPaymentComponent implements OnInit, OnDestroy {
  paymentData: QrPaymentData | null = null;
  loading = true;
  error: string | null = null;
  processing = false;
  
  timeRemaining = 0;
  private intervalId?: number;

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
    return 'Merchant Account'; // Could extract from QR data if needed
  }

  confirmPayment(): void {
    if (!this.paymentData) return;

    this.processing = true;
    const transactionId = parseInt(this.paymentData.paymentId.replace('QR-', ''));

    this.http.post<{status: string, redirectUrl: string, message: string}>(
      'https://localhost:8445/api/qr/confirm',
      { transactionId: transactionId }
    ).subscribe({
      next: (response) => {
        console.log('✅ Payment confirmed:', response.message);
        
        setTimeout(() => {
          window.location.href = response.redirectUrl;
        }, 1000);
      },
      error: (err) => {
        console.error('❌ Confirmation failed:', err);
        this.processing = false;
        alert(err.error?.error || 'Payment confirmation failed');
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
