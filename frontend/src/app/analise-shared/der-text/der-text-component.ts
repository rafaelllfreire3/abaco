import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges
} from '@angular/core';

import { ParseResult, DerTextParser } from './der-text-parser';

@Component({
  selector: 'app-analise-der-text',
  templateUrl: './der-text.component.html'
})
export class DerTextComponent implements OnChanges {

  @Input()
  value: string;

  @Input()
  label: string;

  @Output()
  valueChange: EventEmitter<string> = new EventEmitter<string>();

  parseResult: ParseResult;

  text: string;

  constructor() { }

  getLabel(label) {
    return label;
  }

  ngOnChanges(changes: SimpleChanges) {
    this.text = changes.value.currentValue;
    this.textChanged();
  }

  textChanged() {
    this.valueChange.emit(this.text);
    this.parseResult = DerTextParser.parse(this.text);
  }

  showTotal(): string {
    const parseResult = this.parseResult;
    return parseResult ? parseResult.mostraTotal() : '0';
  }

  deveMostrarDuplicatas(): boolean {
    if (!this.parseResult) {
      return false;
    }
    return this.parseResult.temDuplicatas();
  }
}
