import { Injectable } from '@angular/core';
import { BehaviorSubject, filter, map, Observable } from 'rxjs';
import { ApplicationService } from './application.service';
import { SpinControllerService } from '../api/services';
import { CompareScenariosRequest, GeneratedTextSources, SpinArguments } from '../api/models';
import { LoadingService } from './loading.service';
import { CompareScenarioComponent } from '../components/compare-scenario/compare-scenario.component';

interface SpinState {
  generatedText?: GeneratedTextSources,
  compareScenariosText?: GeneratedTextSources,
  generatedImage?: Blob,
}

@Injectable({
  providedIn: 'root'
})
export class SpinService {
  private readonly state$: BehaviorSubject<SpinState> = new BehaviorSubject<SpinState>({});
  readonly selectGeneratedText$: Observable<GeneratedTextSources> = this.state$.pipe(map(state => state.generatedText)).pipe(filter((p) => !!p), map((p) => p!));
  readonly selectGeneratedComparison$: Observable<GeneratedTextSources> = this.state$.pipe(map(state => state.compareScenariosText)).pipe(filter((p) => !!p), map((p) => p!));
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

  getCompareScenarios(body: CompareScenariosRequest) {
    this.loadingService.show();
    this.applicationService.effect(this.rest.postCompareScenarios({body}), (compareScenariosText) => { this.loadingService.hide(); this.updateState((s) => ({ ...s, compareScenariosText})); });
  }
  
  cleanState(){ this.state$.next({}); }

  updateState(nextState: (previous: SpinState) => SpinState) {
    this.state$.next(nextState(this.state$.getValue()));
  }

  getGeneratedTextInState(): GeneratedTextSources | undefined { return this.state$.getValue().generatedText; }
}