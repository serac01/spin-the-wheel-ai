/* tslint:disable */
/* eslint-disable */
import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';

import { BaseService } from '../base-service';
import { ApiConfiguration } from '../api-configuration';
import { StrictHttpResponse } from '../strict-http-response';
import { RequestBuilder } from '../request-builder';


@Injectable({ providedIn: 'root' })
export class HelloControllerService extends BaseService {
  constructor(config: ApiConfiguration, http: HttpClient) {
    super(config, http);
  }

  /** Path part for operation `hello()` */
  static readonly HelloPath = '/api/hello';

  /**
   * This method provides access to the full `HttpResponse`, allowing access to response headers.
   * To access only the response body, use `hello()` instead.
   *
   * This method doesn't expect any request body.
   */
  hello$Response(
    params?: {
    },
    context?: HttpContext
  ): Observable<StrictHttpResponse<string>> {
    const rb = new RequestBuilder(this.rootUrl, HelloControllerService.HelloPath, 'get');
    if (params) {
    }

    return this.http.request(
      rb.build({ responseType: 'blob', accept: '*/*', context })
    ).pipe(
      filter((r: any): r is HttpResponse<any> => r instanceof HttpResponse),
      map((r: HttpResponse<any>) => {
        return r as StrictHttpResponse<string>;
      })
    );
  }

  /**
   * This method provides access only to the response body.
   * To access the full response (for headers, for example), `hello$Response()` instead.
   *
   * This method doesn't expect any request body.
   */
  hello(
    params?: {
    },
    context?: HttpContext
  ): Observable<string> {
    return this.hello$Response(params, context).pipe(
      map((r: StrictHttpResponse<string>): string => r.body)
    );
  }

}
