import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {

  constructor() { }

  public effect<T>(serviceResult: Observable<T>, onSuccess: (r: T) => void): void {
    serviceResult.subscribe({
        next: result => onSuccess(result),
        error: e => { console.error(e); }
    });
  }
}
