import { Component, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { ConfirmationService } from 'primeng/primeng';
import { DatatableComponent, DatatableClickEvent } from '@basis/angular-components';

import { environment } from '../../environments/environment';
import { TipoFase } from './tipo-fase.model';
import { TipoFaseService } from './tipo-fase.service';
import { ElasticQuery } from '../shared';
import { PageNotificationService } from '../shared';

@Component({
  selector: 'jhi-tipo-fase',
  templateUrl: './tipo-fase.component.html'
})
export class TipoFaseComponent {

  @ViewChild(DatatableComponent) datatable: DatatableComponent;

  searchUrl: string = this.tipoFaseService.searchUrl;

  elasticQuery: ElasticQuery = new ElasticQuery();

  constructor(
    private router: Router,
    private tipoFaseService: TipoFaseService,
    private confirmationService: ConfirmationService,
    private pageNotificationService: PageNotificationService,
  ) {}

  datatableClick(event: DatatableClickEvent) {
    if (!event.selection) {
      return;
    }
    switch (event.button) {
      case 'edit':
        this.router.navigate(['/tipoFase', event.selection.id, 'edit']);
        break;
      case 'delete':
        this.confirmDelete(event.selection.id);
        break;
      case 'view':
        this.router.navigate(['/tipoFase', event.selection.id]);
        break;
    }
  }

  confirmDelete(id: any) {
    this.confirmationService.confirm({
      message: 'Tem certeza que deseja excluir o registro?',
      accept: () => {
        this.tipoFaseService.delete(id).subscribe(() => {
          this.datatable.refresh(this.elasticQuery.query);
          this.pageNotificationService.addDeleteMsg();
        }, error => {
          if(error.status === 500) {
            this.pageNotificationService.addErrorMsg(`A fase não pode ser deletada pois existem Manuais associados a ela.`)
          }
        });
      }
    });
  }
}
