import { Routes } from '@angular/router';


export const routes: Routes = [
  {
    path: 'payment/:paymentId',
    loadComponent: () => import('./components/payment-form/payment-form.component')
      .then(m => m.PaymentFormComponent)
  },
  {
    path: 'qr-payment/:paymentId',
    loadComponent: () => import('./components/qr-payment/qr-payment.component')
      .then(m => m.QrPaymentComponent)
  },
  {
    path: 'qr-scanner/:paymentId',
    loadComponent: () => import('./components/qr-scanner/qr-scanner.component')
      .then(m => m.QrScannerComponent)
  },
  {
    path: '',
    redirectTo: '/payment/example',
    pathMatch: 'full'
  }
];