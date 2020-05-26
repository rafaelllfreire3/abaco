import { IndexadorService } from './../../indexador/indexador.service';
import { TranslateService } from '@ngx-translate/core';
import { Component, AfterViewInit, ViewChild, OnInit } from '@angular/core';
import { ElasticQuery, PageNotificationService, ResponseWrapper } from '../../shared';
import { DatatableClickEvent, DatatableComponent } from '@basis/angular-components';
import { Router } from '@angular/router';
import { SistemaService } from '../../sistema/sistema.service';
import { ConfirmationService } from '../../../../node_modules/primeng/primeng';
import { BaselineService } from '../baseline.service';
import { Sistema } from '../../sistema';

@Component({
    // tslint:disable-next-line:component-selector
    selector: 'jhi-baseline',
    templateUrl: './baseline.component.html'
})
export class BaselineComponent implements OnInit {

    elasticQuery: ElasticQuery = new ElasticQuery();
    @ViewChild(DatatableComponent) datatable: DatatableComponent;
    rowsPerPageOptions: number[] = [5, 10, 20];
    indexList = ['BASE_LINE_ANALITICO', 'BASE_LINE_SINTETICO'];
    public urlBaseLineSintetico = this.baselineService.sinteticosUrl;
    selecionada: boolean;
    nomeSistemas: Array<Sistema>;
    sistema?: Sistema = new Sistema();
    urlBaseline: string;
    enableTable:boolean = false ; 

    constructor(
        private router: Router,
        private baselineService: BaselineService,
        private translate: TranslateService,
        private sistemaService: SistemaService,
        private confirmationService: ConfirmationService,
        private indexadorService: IndexadorService,
    ) {
    }

    getLabel(label) {
        let str: any;
        this.translate.get(label).subscribe((res: string) => {
            str = res;
        }).unsubscribe();
        return str;
    }

    ngOnInit(): void {
        this.recuperarSistema();
       
    }

    public carregarDataTable() {
        this.baselineService.allBaselineSintetico(this.sistema).subscribe((res: ResponseWrapper) => {
            this.datatable.value = res.json;
            this.datatable.reset();
            this.datatable.pDatatableComponent.onRowSelect.subscribe((event) => {
                this.selecionada = false;
            });
            this.datatable.pDatatableComponent.onRowUnselect.subscribe((event) => {
                this.selecionada = true;
            });
        });
    }

    public datatableClick(event: DatatableClickEvent) {
        if (!event.selection) {
            return;
        }
        switch (event.button) {
            case 'view':
                this.router.navigate(['/baseline', event.selection.idsistema, event.selection.equipeResponsavelId]);
                break;
            case 'geraBaselinePdfBrowser':
                this.geraBaselinePdfBrowser(event.selection.idsistema);
                break;
        }
    }

    public geraBaselinePdfBrowser(id) {
        this.baselineService.geraBaselinePdfBrowser(id);
    }
    recuperarSistema() {
        this.sistemaService.dropDown().subscribe(response => {
            this.nomeSistemas = response.json;
            const emptySystem = new Sistema();
            this.nomeSistemas.unshift(emptySystem);
        });
    }
    public changeUrl() {

        let querySearch = '?identificador=';
        querySearch = querySearch.concat((this.sistema && this.sistema.id) ?
            `sistema=${this.sistema.id}&` : '');

        querySearch = (querySearch === '?') ? '' : querySearch;

        querySearch = (querySearch.endsWith('&')) ? querySearch.slice(0, -1) : querySearch;

        return querySearch;
    }

    public performSearch() {
        this.enableTable = true ;
        this.urlBaseline = this.baselineService.resourceUrl + this.changeUrl();
        this.carregarDataTable();
    }
    public limparPesquisa() {
        this.sistema = undefined;
        this.urlBaseline = this.baselineService.resourceUrl + this.changeUrl();
        this.enableTable = false;
        this.recarregarDataTable();
    }
    
    public recarregarDataTable() {
        if(this.datatable){
            this.datatable.url = this.urlBaseline;
            this.datatable.reset();
        }
    }
    public atualizarAnalise() {
        this.confirmationService.confirm({
            message: this.getLabel('Analise.Analise.Mensagens.DesejaAtualizarBaseline'),
            accept: () => {
                this.indexadorService.reindexar(this.indexList).subscribe(()=>{
                });
            }
        });
    }
}
