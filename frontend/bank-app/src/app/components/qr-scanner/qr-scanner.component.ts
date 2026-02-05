import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import jsQR from 'jsqr';

interface QrValidationResult {
  valid: boolean;
  errors: string[];
  parsedData: {
    R: string;
    N: string;
    I: string;
    SF: string;
    S?: string;
    RO?: string;
  };
}

@Component({
  selector: 'app-qr-scanner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './qr-scanner.component.html',
  styleUrls: ['./qr-scanner.component.css']
})
export class QrScannerComponent implements OnInit, OnDestroy {
  @ViewChild('video', { static: false }) videoElement!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas', { static: false }) canvasElement!: ElementRef<HTMLCanvasElement>;

  paymentId: string | null = null;
  stream: MediaStream | null = null;
  scanning = false;
  scanned = false;
  error: string | null = null;
  
  qrData: QrValidationResult | null = null;
  processing = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.paymentId = this.route.snapshot.paramMap.get('paymentId');
    this.startScanning();
  }

  ngOnDestroy(): void {
    this.stopScanning();
  }

  async startScanning(): Promise<void> {
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' } 
      });

      if (this.videoElement) {
        this.videoElement.nativeElement.srcObject = this.stream;
        this.videoElement.nativeElement.play();
        this.scanning = true;
        this.scanQRCode();
      }
    } catch (err) {
      console.error('Error accessing camera', err);
      this.error = 'Cannot access camera. Please allow camera permissions.';
    }
  }

  stopScanning(): void {
    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop());
      this.stream = null;
    }
    this.scanning = false;
  }

  scanQRCode(): void {
    if (!this.scanning || this.scanned) return;

    const video = this.videoElement.nativeElement;
    const canvas = this.canvasElement.nativeElement;
    const context = canvas.getContext('2d');

    if (video.readyState === video.HAVE_ENOUGH_DATA) {
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      context?.drawImage(video, 0, 0, canvas.width, canvas.height);

      const imageData = context?.getImageData(0, 0, canvas.width, canvas.height);
      
      if (imageData) {
        const code = jsQR(imageData.data, imageData.width, imageData.height);

        if (code) {
          console.log('QR Code detected:', code.data);
          this.handleQRCode(code.data);
          return;
        }
      }
    }

    requestAnimationFrame(() => this.scanQRCode());
  }

  handleQRCode(qrPayload: string): void {
    this.scanned = true;
    this.stopScanning();

    this.http.post<QrValidationResult>('https://localhost:8445/api/qr/validate', {
      payload: qrPayload
    }).subscribe({
      next: (result) => {
        if (result.valid) {
          console.log('Valid QR code:', result);
          this.qrData = result;
          this.processPayment();
        } else {
          this.error = 'Invalid QR code: ' + result.errors.join(', ');
        }
      },
      error: (err) => {
        console.error('Validation error', err);
        this.error = 'Failed to validate QR code';
      }
    });
  }

  processPayment(): void {
    if (!this.qrData || !this.paymentId) return;

    this.processing = true;

    const transactionId = this.paymentId.replace('QR-', '');

    this.http.post('https://localhost:8445/api/qr/process', {
      transactionId: transactionId,
      qrPayload: JSON.stringify(this.qrData.parsedData)
    }).subscribe({
      next: () => {
        console.log('Payment processed successfully');
      },
      error: (err) => {
        console.error('Payment processing failed', err);
        this.error = 'Payment processing failed';
        this.processing = false;
      }
    });
  }

  resetScanner(): void {
    this.scanned = false;
    this.error = null;
    this.qrData = null;
    this.startScanning();
  }
}
