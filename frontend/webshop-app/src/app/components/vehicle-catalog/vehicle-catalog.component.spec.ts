import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VehicleCatalogComponent } from './vehicle-catalog.component';

describe('VehicleCatalogComponent', () => {
  let component: VehicleCatalogComponent;
  let fixture: ComponentFixture<VehicleCatalogComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [VehicleCatalogComponent]
    });
    fixture = TestBed.createComponent(VehicleCatalogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
