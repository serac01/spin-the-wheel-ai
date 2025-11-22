import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-control-buttons',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './control-buttons.component.html',
  styleUrls: ['./control-buttons.component.css']
})
export class ControlButtonsComponent {
  @Input() canCompare: boolean = false;
  @Input() canRestart: boolean = false;

  constructor() { }

  compare() {

  }

  restart() {
    
  }

}
