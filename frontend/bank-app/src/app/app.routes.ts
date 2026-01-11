import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'payment/:paymentId',
    loadComponent: () => import('./components/payment-form/payment-form.component')
      .then(m => m.PaymentFormComponent)
  },
  {
    path: '',
    redirectTo: '/payment/example',
    pathMatch: 'full'
  }
];