import { Component, HostListener, Input, OnInit } from '@angular/core';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { SpinService } from '../../services/spin.service';
import { CommonModule } from '@angular/common';
import { GeneratedTextSources, SpinArguments } from '../../api/models';
import { Observable } from 'rxjs';
import { NgIconComponent, provideIcons } from '@ng-icons/core';
import { iconoirArrowUpRight, iconoirXmark } from "@ng-icons/iconoir";

@Component({
  selector: 'app-story',
  standalone: true,
  imports: [CommonModule, NgIconComponent],
  templateUrl: './story.component.html',
  styleUrls: ['./story.component.css'],
  providers: [provideIcons({ iconoirArrowUpRight, iconoirXmark })]
})
export class StoryComponent implements OnInit {
  @Input() body: SpinArguments | undefined;
  @Input() isComparing: boolean = false;
  readonly selectGeneratedText$: Observable<GeneratedTextSources>
  imageUrl: SafeUrl | null = null;
  isTooltipOpen = false;

  constructor(private spinService: SpinService, private sanitizer: DomSanitizer) {
    this.selectGeneratedText$ = spinService.selectGeneratedText$;
  }

  ngOnInit() {
    if(this.body){
      this.spinService.getGeneratedImage(this.body)
      this.spinService.getGeneratedText(this.body)
      this.spinService.selectGeneratedImage$.subscribe(blob => {
        const url = URL.createObjectURL(blob);
        this.imageUrl = this.sanitizer.bypassSecurityTrustUrl(url);
      });
    }
  }

  toggleTooltip() {
    this.isTooltipOpen = !this.isTooltipOpen;
  }

  @HostListener('document:click', ['$event'])
  onClick(event: Event) {
    if (!(event.target as HTMLElement).closest('.tooltip-icon') &&
        !(event.target as HTMLElement).closest('.tooltip-box')) {
      this.isTooltipOpen = false;
    }
  }

}
