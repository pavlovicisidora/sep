import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
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
export class QrPaymentComponent implements OnInit {
  paymentData: QrPaymentData | null = null;
  loading = true;
  error: string | null = null;
  expired = false;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const paymentId = this.route.snapshot.paramMap.get('paymentId');
    
    if (paymentId) {
      this.loadQrPaymentData(paymentId);
      this.startExpiryCheck();
    } else {
      this.error = 'Invalid payment ID';
      this.loading = false;
    }
  }

  loadQrPaymentData(paymentId: string): void {
    this.http.get<QrPaymentData>(`https://localhost:8445/api/qr/${paymentId}`)
      .subscribe({
        next: (data) => {
          this.paymentData = data;
          this.loading = false;
          this.checkExpiry();
        },
        error: (err) => {
          console.error('Error loading QR payment data', err);
          this.error = 'Failed to load payment information';
          this.loading = false;
        }
      });
  }

  startExpiryCheck(): void {
    setInterval(() => {
      this.checkExpiry();
    }, 10000);
  }

  checkExpiry(): void {
    if (!this.paymentData) return;
    
    const expiryTime = new Date(this.paymentData.expiresAt).getTime();
    const now = Date.now();
    
    if (now > expiryTime) {
      this.expired = true;
    }
  }

  getQrImageSrc(): string {
    if (!this.paymentData) return '';
    return `data:image/png;base64,${this.paymentData.qrCodeBase64}`;
  }

  getRemainingTime(): string {
    if (!this.paymentData) return '';
    
    const expiryTime = new Date(this.paymentData.expiresAt).getTime();
    const now = Date.now();
    const remainingMs = expiryTime - now;
    
    if (remainingMs <= 0) return 'Expired';
    
    const minutes = Math.floor(remainingMs / 60000);
    const seconds = Math.floor((remainingMs % 60000) / 1000);
    
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
