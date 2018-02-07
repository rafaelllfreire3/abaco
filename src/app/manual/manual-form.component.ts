import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Response } from '@angular/http';
import { Observable, Subscription } from 'rxjs/Rx';
import { SelectItem } from 'primeng/primeng';

import { Manual } from './manual.model';
import { ManualService } from './manual.service';
import { EsforcoFaseService } from '../esforco-fase/esforco-fase.service';
import { ResponseWrapper } from '../shared';
import { EsforcoFase } from '../esforco-fase/esforco-fase.model';
import { TipoFaseService } from '../tipo-fase/tipo-fase.service';
import { TipoFase } from '../tipo-fase/tipo-fase.model';
import { DatatableClickEvent } from '@basis/angular-components';
import { ConfirmationService } from 'primeng/components/common/confirmationservice';
import { FatorAjuste, TipoFatorAjuste } from '../fator-ajuste/fator-ajuste.model';
import { PageNotificationService } from '../shared/page-notification.service';
import { UploadService } from '../upload/upload.service';
import { FileUpload } from 'primeng/primeng';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'jhi-manual-form',
  templateUrl: './manual-form.component.html',
})
export class ManualFormComponent implements OnInit, OnDestroy {
  manual: Manual;
  isSaving: boolean;
  private routeSub: Subscription;
  arquivoManual: File;
  esforcoFases: Array<EsforcoFase>;
  showDialogPhaseEffort: boolean = false;
  showDialogEditPhaseEffort: boolean = false;
  showDialogCreateAdjustFactor: boolean = false;
  showDialogEditAdjustFactor: boolean = false;
  tipoFases: Array<TipoFase> = [];
  percentual: number;
  newPhaseEffort: EsforcoFase = new EsforcoFase();
  editedPhaseEffort: EsforcoFase = new EsforcoFase();
  newAdjustFactor: FatorAjuste = new FatorAjuste();
  editedAdjustFactor: FatorAjuste = new FatorAjuste();
  adjustTypes: Array<any> = [
    {
      label: 'Percentual',
      value: 'PERCENTUAL',
    },
    {
      label: 'Unitário',
      value: 'UNITARIO',
    },
  ]
  invalidFields: Array<string> = [];

  @ViewChild('fileInput') fileInput: FileUpload;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private manualService: ManualService,
    private esforcoFaseService: EsforcoFaseService,
    private tipoFaseService: TipoFaseService,
    private confirmationService: ConfirmationService,
    private pageNotificationService: PageNotificationService,
    private uploadService: UploadService,
    private translate: TranslateService
  ) {
    translate.setDefaultLang('pt');
    translate.use(sessionStorage.getItem('language'));
  }

  ngOnInit() {
    this.isSaving = false;
    this.routeSub = this.route.params.subscribe(params => {
      this.manual = new Manual();
      if (params['id']) {
        this.manualService.find(params['id']).subscribe(manual => {
          this.manual = manual;
          this.getFile();
        });
      }
    });

    this.tipoFaseService.query().subscribe((response: ResponseWrapper) => {
      this.tipoFases = response.json;
    });
  }

  save() {
    this.isSaving = true;
    this.manual.valorVariacaoEstimada = this.manual.valorVariacaoEstimada;
    this.manual.valorVariacaoIndicativa = this.manual.valorVariacaoIndicativa;

    console.log(this.manual);
    if (this.manual.id !== undefined) {
      this.manualService.find(this.manual.id).subscribe(response => {
        if(this.arquivoManual !== undefined) {
          this.uploadService.uploadFile(this.arquivoManual).subscribe(response => {
            this.manual.arquivoManualId = JSON.parse(response["_body"]).id;
            this.subscribeToSaveResponse(this.manualService.update(this.manual));
          })
        } else {
          this.subscribeToSaveResponse(this.manualService.update(this.manual));
        }
      })

    } else {
      if(this.arquivoManual !== undefined) {
        if(this.checkRequiredFields()) {
          this.uploadService.uploadFile(this.arquivoManual).subscribe(response => {
            this.manual.arquivoManualId = JSON.parse(response["_body"]).id;
            this.subscribeToSaveResponse(this.manualService.create(this.manual));
          });
        } else {
          this.pageNotificationService.addErrorMsg('Campos inválidos: ' + this.getInvalidFieldsString());
          this.invalidFields = [];
        }
      } else {
        this.pageNotificationService.addErrorMsg('Campo Arquivo Manual está inválido!');
      }
    }
  }

  private checkRequiredFields(): boolean {
      let isFieldsValid = false;
      console.log(this.manual);
      if ( isNaN(this.manual.valorVariacaoEstimada)) (this.invalidFields.push('Valor Variação Estimada'));
      if ( isNaN(this.manual.valorVariacaoIndicativa)) (this.invalidFields.push('Valor Variação Inidicativa'));

      isFieldsValid = (this.invalidFields.length === 0);

      return isFieldsValid;
  }

  private getInvalidFieldsString(): string {
    let invalidFieldsString = "";
    this.invalidFields.forEach(invalidField => {
      if(invalidField === this.invalidFields[this.invalidFields.length-1]) {
        invalidFieldsString = invalidFieldsString + invalidField;
      } else {
        invalidFieldsString = invalidFieldsString + invalidField + ', ';
      }
    });

    return invalidFieldsString;
  }

  private subscribeToSaveResponse(result: Observable<Manual>) {
    result.subscribe((res: Manual) => {
      this.isSaving = false;
      this.router.navigate(['/manual']);
      this.pageNotificationService.addCreateMsg();
    }, (error: Response) => {
      alert(error);
      this.isSaving = false;
      switch(error.status) {
        case 400: {
          let invalidFieldNamesString = "";
          const fieldErrors = JSON.parse(error["_body"]).fieldErrors;
          invalidFieldNamesString = this.pageNotificationService.getInvalidFields(fieldErrors);
          this.pageNotificationService.addErrorMsg("Campos inválidos: " + invalidFieldNamesString);
        }
      }
    });
  }

  ngOnDestroy() {
    this.routeSub.unsubscribe();
  }

  uploadFile(event) {
    this.arquivoManual = event.files[0];
  }

  datatableClick(event: DatatableClickEvent) {
    if (!event.selection) {
      return;
    }
    console.log(event.selection);
    switch (event.button) {
      case 'edit':
        this.editedPhaseEffort = event.selection.clone();
        this.openDialogEditPhaseEffort();
        break;
      case 'delete':
        console.log(event.selection);
        this.editedPhaseEffort = event.selection.clone();
        this.confirmDeletePhaseEffort();
    }
  }

  adjustFactorDatatableClick(event: DatatableClickEvent) {
    if (!event.selection) {
      return;
    }
    switch (event.button) {
      case 'edit':
        this.editedAdjustFactor = event.selection.clone();
        (this.editedAdjustFactor.fator > 0 && this.editedAdjustFactor.fator < 1) ?
          (this.editedAdjustFactor.fator = this.editedAdjustFactor.fator) : (this.editedAdjustFactor = this.editedAdjustFactor);
        this.openDialogEditAdjustFactor();
        break;
      case 'delete':
        console.log(event.selection)
        this.editedAdjustFactor = event.selection.clone();
        this.confirmDeleteAdjustFactor();
    }
  }

  isPercentualEnum(value: TipoFatorAjuste) {

    return (value !== undefined) ? (value.toString() === 'PERCENTUAL') : (false);
  }

  isUnitaryEnum(value: TipoFatorAjuste) {
    return (value !== undefined) ? (value.toString() === 'UNITARIO') : (false);
  }
  confirmDeletePhaseEffort() {
    this.confirmationService.confirm({
      message: 'Tem certeza que deseja excluir o Esforço por fase ' + this.editedPhaseEffort.fase.nome + '?',
      accept: () => {
        this.manual.deleteEsforcoFase(this.editedPhaseEffort);
        this.editedPhaseEffort = new EsforcoFase();
      }
    });
  }

  confirmDeleteAdjustFactor() {
    this.confirmationService.confirm({
      message: 'Tem certeza que deseja excluir o Fator de Ajuste ' + this.editedAdjustFactor.nome + '?',
      accept: () => {
        this.manual.deleteFatoresAjuste(this.editedAdjustFactor);
        this.editedAdjustFactor = new FatorAjuste();
      }
    });
  }

  openDialogPhaseEffort() {
    this.newPhaseEffort = new EsforcoFase();
    this.showDialogPhaseEffort = true;
  }

  openDialogEditPhaseEffort() {
      this.showDialogEditPhaseEffort = true;
  }

  editPhaseEffort() {
    this.manual.updateEsforcoFases(this.editedPhaseEffort);
    this.closeDialogEditPhaseEffort();
  }

  editAdjustFactor() {
    this.manual.updateFatoresAjuste(this.editedAdjustFactor);
    this.closeDialogEditAdjustFactor();
  }

  closeDialogPhaseEffort() {
    this.newPhaseEffort = new EsforcoFase();
    this.showDialogPhaseEffort = false;
  }

  closeDialogEditPhaseEffort() {
    this.editedPhaseEffort = new EsforcoFase();
    this.showDialogEditPhaseEffort = false;
  }

  addPhaseEffort() {
    this.newPhaseEffort.esforco = this.newPhaseEffort.esforco;
    this.manual.addEsforcoFases(this.newPhaseEffort);
    this.closeDialogPhaseEffort();
  }

  getPhaseEffortTotalPercentual() {
    let total = 0;
    this.manual.esforcoFases.forEach(each => {
      (each.esforco !== undefined) ? (total = total + each.esforcoFormatado) : (total = total);
    });

    return total;
  }

  openDialogCreateAdjustFactor() {
    this.showDialogCreateAdjustFactor = true;
  }

  closeDialogCreateAdjustFactor() {
    this.showDialogCreateAdjustFactor = false;
    this.newAdjustFactor = new FatorAjuste();
  }

  openDialogEditAdjustFactor() {
    this.showDialogEditAdjustFactor = true;
  }

  closeDialogEditAdjustFactor() {
      this.showDialogEditAdjustFactor = false;
      this.editedAdjustFactor = new FatorAjuste();
  }

  addAdjustFactor() {
    this.newAdjustFactor.ativo = true;
    this.manual.addFatoresAjuste(this.newAdjustFactor);
    this.closeDialogCreateAdjustFactor();
  }

  getFile() {
    this.uploadService.getFile(this.manual.arquivoManualId).subscribe(response => {

      let fileInfo;
      this.uploadService.getFileInfo(this.manual.arquivoManualId).subscribe(response => {
        fileInfo = response;

        this.fileInput.files.push(new File([response["_body"]], fileInfo["originalName"]));
      });
    });
  }

  getFileInfo() {
    return this.uploadService.getFile(this.manual.arquivoManualId).subscribe(response => {
      return response;
    })
  }
}
