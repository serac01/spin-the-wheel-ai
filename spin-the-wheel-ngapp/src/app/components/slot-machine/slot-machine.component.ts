import { Component, Input, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { Gender, SpinArguments } from '../../api/models';
import { CommonModule } from '@angular/common';
import { StoryComponent } from '../story/story.component';
import { SpinService } from '../../services/spin.service';

@Component({
  selector: 'app-slot-machine',
  standalone: true,
  imports: [CommonModule, StoryComponent],
  templateUrl: './slot-machine.component.html',
  styleUrls: ['./slot-machine.component.css'],
  providers: [SpinService]
})
export class SlotMachineComponent implements OnInit, OnChanges {
  @Input() places: string[] | null | undefined;
  @Input() times: number[] | null | undefined;
  @Input() genders: Gender[] | null | undefined;
  @Input() isComparing: boolean = false;
  body: SpinArguments = {};

  slots: any[] = [];
  spinning = false;
  resultVisible = false;
  resultImage = '';
  resultText = '';

  ngOnInit() {
    this.slots = [
      { list: [], transform: 'translateY(0px)', duration: 0, items: this.places },
      { list: [], transform: 'translateY(0px)', duration: 0, items: this.times },
      { list: [], transform: 'translateY(0px)', duration: 0, items: this.genders }
    ];
  }

  ngOnChanges() {
    if (this.slots.length) {
      this.slots[0].items = this.places ?? [];
      this.slots[1].items = this.times ?? [];
      this.slots[2].items = this.genders ?? [];
    }
  }

  spin() {
    if (this.spinning) return;
    this.resultVisible = false;
    this.spinning = true;
    console.log(this.times,this.places,this.genders)
    this.slots.forEach((slot: any, index) => {
      const items = Array.isArray(slot.items) ? slot.items : [];

      if (items.length === 0) return;

      slot.list = [...items, ...items, ...items];

      const finalIndex = Math.floor(Math.random() * items.length);
      const spins = 3;
      const itemHeight = 60;
      const translate = -(spins * items.length + finalIndex) * itemHeight;

      slot.duration = 1000 + index * 300;
      slot.transform = `translateY(${translate}px)`;

      setTimeout(() => {
        slot.list = [items[finalIndex]];
        slot.transform = 'translateY(0px)';

        if (index === this.slots.length - 1) {
          this.body.city = this.slots[0].list[0]
          this.body.year = this.slots[1].list[0]
          this.body.gender = this.slots[2].list[0]
          this.resultVisible = true;
          this.spinning = false;
        }
      }, slot.duration);
    });
  }

  formatItem(item: any) {
    if (typeof item === 'string' || typeof item === 'number') return item;
    if (item && item.description) return item.description;
    return '';
  }
}
