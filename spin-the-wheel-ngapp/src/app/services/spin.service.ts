import { Injectable } from '@angular/core';
import { BehaviorSubject, filter, map, Observable } from 'rxjs';
import { ApplicationService } from './application.service';
import { SpinControllerService } from '../api/services';
import { GeneratedTextSources, SpinArguments } from '../api/models';
import { LoadingService } from './loading.service';

interface SpinState {
  generatedText?: GeneratedTextSources,
  generatedImage?: Blob,
}

@Injectable({
  providedIn: 'root'
})
export class SpinService {
  private readonly state$: BehaviorSubject<SpinState> = new BehaviorSubject<SpinState>({});
  readonly selectGeneratedText$: Observable<GeneratedTextSources> = this.state$.pipe(map(state => state.generatedText)).pipe(filter((p) => !!p), map((p) => p!));
  readonly selectGeneratedImage$: Observable<Blob> = this.state$.pipe(map(state => state.generatedImage)).pipe(filter((p) => !!p), map((p) => p!));
 
  constructor(private applicationService: ApplicationService, private rest: SpinControllerService, private loadingService: LoadingService) { }
  
  getGeneratedImage(args: SpinArguments) {
    this.loadingService.show();
    this.applicationService.effect(
      this.rest.postGeneratedImage({ body: args }),
      (blob) => {
        this.updateState(s => ({ ...s, generatedImage: blob }));
        this.loadingService.hide();
      }
    );
  }

  getGeneratedText(body: SpinArguments) {
    this.loadingService.show();
    this.applicationService.effect(this.rest.postGeneratedText({body}), (generatedText) => { this.loadingService.hide(); this.updateState((s) => ({ ...s, generatedText})); });
  }
  
  cleanState(){ this.state$.next({}); }

  updateState(nextState: (previous: SpinState) => SpinState) {
    this.state$.next(nextState(this.state$.getValue()));
  }
}