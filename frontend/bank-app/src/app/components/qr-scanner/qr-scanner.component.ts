import { Component, HostListener, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  imports: [CommonModule, FormsModule],
  templateUrl: './qr-scanner.component.html',
  styleUrls: ['./qr-scanner.component.css']
})
export class QrScannerComponent implements AfterViewInit, OnDestroy {
  @ViewChild('video', { static: false }) videoElement?: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas', { static: false }) canvasElement?: ElementRef<HTMLCanvasElement>;

  paymentId: string | null = null;
  stream: MediaStream | null = null;
  scanning = false;
  scanned = false;
  error: string | null = null;
  cameraReady = false;
  validationSuccess = false;

  qrData: QrValidationResult | null = null;
  processing = false;
  accountNumber = '';
  accountError: string | null = null;

  private animationFrameId?: number;

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
  ) {
    this.paymentId = this.route.snapshot.paramMap.get('paymentId');
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.initCamera(), 500);
  }

  ngOnDestroy(): void {
    this.cleanup();
  }

  private async initCamera(): Promise<void> {
    if (!this.videoElement?.nativeElement) {
      console.error('Video element not found');
      this.error = 'Camera initialization failed - video element missing';
      return;
    }

    try {
      let stream: MediaStream;

      try {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: { exact: 'environment' } }
        });
      } catch {
        stream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'user' }
        });
      }

      this.stream = stream;
      const video = this.videoElement.nativeElement;
      video.srcObject = stream;

      await new Promise<void>((resolve) => {
        video.onloadedmetadata = () => {
          video.play().then(() => {
            this.cameraReady = true;
            this.scanning = true;
            resolve();
          });
        };
      });

      this.scanLoop();

    } catch (err: any) {
      console.error('Camera error:', err);

      if (err.name === 'NotAllowedError') {
        this.error = 'Camera access denied. Please allow camera access in browser settings.';
      } else if (err.name === 'NotFoundError') {
        this.error = 'No camera found. Please connect a camera.';
      } else {
        this.error = `Camera error: ${err.message}`;
      }
    }
  }

  private scanLoop(): void {
    if (!this.scanning || this.scanned) return;

    const video = this.videoElement?.nativeElement;
    const canvas = this.canvasElement?.nativeElement;

    if (!video || !canvas || video.readyState !== video.HAVE_ENOUGH_DATA) {
      this.animationFrameId = requestAnimationFrame(() => this.scanLoop());
      return;
    }

    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return;

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'dontInvert'
    });

    if (code) {
      console.log('QR Code detected:', code.data);
      this.handleQRCode(code.data);
      return;
    }

    this.animationFrameId = requestAnimationFrame(() => this.scanLoop());
  }

  private handleQRCode(qrPayload: string): void {
    this.scanned = true;
    this.scanning = false;

    this.http.post<QrValidationResult>('https://localhost:8445/api/qr/validate', {
      payload: qrPayload
    }).subscribe({
      next: (result) => {
        if (result.valid) {
          this.qrData = result;
          this.showValidationSuccess();
        } else {
          this.error = `Invalid QR code: ${result.errors.join(', ')}`;
        }
      },
      error: (err) => {
        console.error('Validation error:', err);
        this.error = 'Failed to validate QR code';
      }
    });
  }

  private showValidationSuccess(): void {
    this.validationSuccess = true;
    console.log('Valid QR Code detected!', this.qrData);
  }

  confirmPayment(): void {
    if (!this.paymentId || !this.qrData) return;

    if (!this.accountNumber.trim()) {
      this.accountError = 'Please enter your bank account number';
      return;
    }

    this.processing = true;
    this.accountError = null;
    const transactionId = parseInt(this.paymentId.replace('QR-', ''));

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
        this.accountError = err.error?.error || 'Payment confirmation failed';

        if (err.error?.redirectUrl) {
          setTimeout(() => {
            window.location.href = err.error.redirectUrl;
          }, 2000);
        }
      }
    });
  }

  resetScanner(): void {
    this.scanned = false;
    this.error = null;
    this.qrData = null;
    this.processing = false;
    this.validationSuccess = false;
    this.accountNumber = '';
    this.accountError = null;
    this.scanning = true;
    this.scanLoop();
  }

  private cleanup(): void {
    if (this.animationFrameId) {
      cancelAnimationFrame(this.animationFrameId);
    }
    if (this.stream) {
      this.stream.getTracks().forEach(track => track.stop());
    }
  }
}
