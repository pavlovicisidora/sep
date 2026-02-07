import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QrPaymentComponent } from './qr-payment.component';

describe('QrPaymentComponent', () => {
  let component: QrPaymentComponent;
  let fixture: ComponentFixture<QrPaymentComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [QrPaymentComponent]
    });
    fixture = TestBed.createComponent(QrPaymentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
