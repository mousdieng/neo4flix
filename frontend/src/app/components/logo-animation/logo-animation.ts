import { Component, OnInit, output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-logo-animation',
  imports: [CommonModule],
  templateUrl: './logo-animation.html',
  styleUrl: './logo-animation.css'
})
export class LogoAnimation implements OnInit {
  public animationComplete = output<void>();
  public showAnimation = true;

  ngOnInit(): void {
    // Animation duration: 3.5 seconds total
    // Logo fades in and scales: 1s
    // Logo holds: 1.5s
    // Logo fades out: 1s
    setTimeout(() => {
      this.showAnimation = false;
      this.animationComplete.emit();
    }, 3500);
  }
}
