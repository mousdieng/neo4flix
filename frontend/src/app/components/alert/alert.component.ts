import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../services/alert.service';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.css']
})
export class AlertComponent {
  alertService = inject(AlertService);

  dismiss(id: string): void {
    this.alertService.dismiss(id);
  }

  executeAction(callback: () => void, id: string): void {
    callback();
    this.dismiss(id);
  }
}
