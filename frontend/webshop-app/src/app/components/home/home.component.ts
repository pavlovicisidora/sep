import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="home-container">
      <h1>Car Rental Agency</h1>
      <p>Welcome to our car rental service!</p>
      <p class="note">Full WebShop UI coming soon...</p>
    </div>
  `,
  styles: [`
    .home-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 20px;
    }
    h1 {
      font-size: 48px;
      margin: 0 0 20px;
    }
    p {
      font-size: 20px;
      margin: 10px 0;
    }
    .note {
      margin-top: 40px;
      opacity: 0.7;
      font-style: italic;
    }
  `]
})
export class HomeComponent {}
