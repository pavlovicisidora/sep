import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/home/home.component')
      .then(m => m.HomeComponent)
  },
  {
    path: 'payment/success',
    loadComponent: () => import('./components/payment-success/payment-success.component')
      .then(m => m.PaymentSuccessComponent)
  },
  {
    path: 'payment/failed',
    loadComponent: () => import('./components/payment-failed/payment-failed.component')
      .then(m => m.PaymentFailedComponent)
  }
];
