import { Component, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { SpinService } from '../../services/spin.service';
import { CommonModule } from '@angular/common';
import { GeneratedTextSources, SpinArguments } from '../../api/models';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-story',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './story.component.html',
  styleUrls: ['./story.component.css']
})
export class StoryComponent implements OnInit {
  @Input() body: SpinArguments | undefined;
  @Input() isComparing: boolean = false;
  imageUrl: SafeUrl | null = null;
  readonly selectGeneratedText$: Observable<GeneratedTextSources>

  constructor(private spinService: SpinService, private sanitizer: DomSanitizer) {
    this.selectGeneratedText$ = spinService.selectGeneratedText$;
    spinService.selectGeneratedText$.subscribe((test) => console.log(test))
  }

  ngOnInit() {
    if(this.body){
      console.log(this.body)
      this.spinService.getGeneratedImage(this.body)
      this.spinService.getGeneratedText(this.body)
      this.spinService.selectGeneratedImage$.subscribe(blob => {
        console.log(blob)
        const url = URL.createObjectURL(blob);
        this.imageUrl = this.sanitizer.bypassSecurityTrustUrl(url);
      });
    }
  }

}
