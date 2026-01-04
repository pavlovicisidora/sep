import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component')
      .then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./components/register/register.component')
      .then(m => m.RegisterComponent)
  },
  {
    path: 'vehicles',
    loadComponent: () => import('./components/vehicle-catalog/vehicle-catalog.component')
      .then(m => m.VehicleCatalogComponent),
    canActivate: [authGuard]
  },
  {
    path: 'my-orders',
    loadComponent: () => import('./components/my-orders/my-orders.component')
      .then(m => m.MyOrdersComponent),
    canActivate: [authGuard]
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
  },
  {
    path: '',
    redirectTo: '/vehicles',
    pathMatch: 'full'
  }
];
