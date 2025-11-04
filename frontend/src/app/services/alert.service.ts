import { Injectable, signal } from '@angular/core';

export type AlertType = 'success' | 'error' | 'warning' | 'info';

export interface Alert {
  id: string;
  type: AlertType;
  message: string;
  title?: string;
  duration?: number;
  action?: {
    label: string;
    callback: () => void;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alerts = signal<Alert[]>([]);
  public alerts$ = this.alerts.asReadonly();

  private generateId(): string {
    return `alert-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  show(type: AlertType, message: string, options?: {
    title?: string;
    duration?: number;
    action?: { label: string; callback: () => void };
  }): string {
    const alert: Alert = {
      id: this.generateId(),
      type,
      message,
      title: options?.title,
      duration: options?.duration ?? 5000,
      action: options?.action
    };

    this.alerts.update(alerts => [...alerts, alert]);

    if (alert.duration && alert.duration > 0) {
      setTimeout(() => this.dismiss(alert.id), alert.duration);
    }

    return alert.id;
  }

  success(message: string, options?: { title?: string; duration?: number }): string {
    return this.show('success', message, {
      title: options?.title ?? 'Success',
      duration: options?.duration
    });
  }

  error(message: string, options?: { title?: string; duration?: number; action?: { label: string; callback: () => void } }): string {
    return this.show('error', message, {
      title: options?.title ?? 'Error',
      duration: options?.duration ?? 7000,
      action: options?.action
    });
  }

  warning(message: string, options?: { title?: string; duration?: number }): string {
    return this.show('warning', message, {
      title: options?.title ?? 'Warning',
      duration: options?.duration
    });
  }

  info(message: string, options?: { title?: string; duration?: number }): string {
    return this.show('info', message, {
      title: options?.title ?? 'Info',
      duration: options?.duration
    });
  }

  dismiss(id: string): void {
    this.alerts.update(alerts => alerts.filter(alert => alert.id !== id));
  }

  dismissAll(): void {
    this.alerts.set([]);
  }
}
