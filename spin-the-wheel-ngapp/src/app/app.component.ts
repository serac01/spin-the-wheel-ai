import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { SlotMachineComponent } from './components/slot-machine/slot-machine.component';
import { ParameterizationService } from './services/parameterization.service';
import { Observable } from 'rxjs';
import { Gender } from './api/models';
import { ControlButtonsComponent } from './components/control-buttons/control-buttons.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, SlotMachineComponent, ControlButtonsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  isComparing: boolean = false
  readonly selectPlaces$: Observable<string[]>
  readonly selectTimes$: Observable<number[]>
  readonly selectGenders$: Observable<Gender[]>

  constructor(private parameterizationService: ParameterizationService) {
    this.selectPlaces$ = parameterizationService.selectPlaces$;
    this.selectTimes$ = parameterizationService.selectTimes$;
    this.selectGenders$ = parameterizationService.selectGenders$;
    parameterizationService.selectPlaces$.subscribe((test) => console.log(test))
  }

  ngOnInit() {
    this.parameterizationService.getGenders();
    this.parameterizationService.getPlaces();
    this.parameterizationService.getTimes();
  }
}
