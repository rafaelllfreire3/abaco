package br.com.basis.abaco.web.rest;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import br.com.basis.abaco.domain.Authority;
import br.com.basis.abaco.domain.Analise;
import br.com.basis.abaco.domain.FuncaoDados;
import br.com.basis.abaco.domain.FuncaoDadosVersionavel;
import br.com.basis.abaco.domain.Sistema;
import br.com.basis.abaco.domain.User;
import br.com.basis.abaco.repository.UserRepository;
import br.com.basis.abaco.repository.search.TipoEquipeSearchRepository;
import br.com.basis.abaco.repository.search.UserSearchRepository;
import br.com.basis.abaco.security.SecurityUtils;
import org.springframework.core.convert.converter.Converter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import br.com.basis.abaco.utils.PageUtils;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import br.com.basis.abaco.service.exception.RelatorioException;
import br.com.basis.abaco.service.relatorio.RelatorioAnaliseColunas;
import br.com.basis.abaco.utils.AbacoUtil;
import br.com.basis.dynamicexports.service.DynamicExportsService;
import br.com.basis.dynamicexports.util.DynamicExporter;
import net.sf.dynamicreports.report.exception.DRException;
import net.sf.jasperreports.engine.JRException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

import br.com.basis.abaco.domain.enumeration.TipoRelatorio;
import br.com.basis.abaco.reports.rest.RelatorioAnaliseRest;
import br.com.basis.abaco.repository.AnaliseRepository;
import br.com.basis.abaco.repository.FuncaoDadosVersionavelRepository;
import br.com.basis.abaco.repository.search.AnaliseSearchRepository;
import br.com.basis.abaco.web.rest.util.HeaderUtil;
import br.com.basis.abaco.web.rest.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import io.swagger.annotations.ApiParam;

/**
 * REST controller for managing Analise.
 */
@RestController
@RequestMapping("/api")
public class AnaliseResource {

    private static final String QUERY_MSG_CONST = "REST request to search for a page of Analises for query {}";

    private final Logger log = LoggerFactory.getLogger(AnaliseResource.class);

    private static final String ENTITY_NAME = "analise";

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final String ROLE_USER = "ROLE_USER";

    private static final String PAGE = "page";

    private final AnaliseRepository analiseRepository;

    private final UserRepository userRepository;

    private final AnaliseSearchRepository analiseSearchRepository;

    private final UserSearchRepository userSearchRepository;

    private final TipoEquipeSearchRepository equipeSearchRepository;

    private final FuncaoDadosVersionavelRepository funcaoDadosVersionavelRepository;

    private RelatorioAnaliseRest relatorioAnaliseRest;

    private DynamicExportsService dynamicExportsService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    /**
     * Método construtor.
     * @param analiseRepository
     * @param analiseSearchRepository
     * @param funcaoDadosVersionavelRepository
     */
    public AnaliseResource(
             AnaliseRepository analiseRepository
            ,AnaliseSearchRepository analiseSearchRepository
            ,FuncaoDadosVersionavelRepository funcaoDadosVersionavelRepository
            , DynamicExportsService dynamicExportsService
            , UserRepository userRepository
            , UserSearchRepository userSearchRepository
            , TipoEquipeSearchRepository equipeSearchRepository) {
        this.analiseRepository = analiseRepository;
        this.analiseSearchRepository = analiseSearchRepository;
        this.funcaoDadosVersionavelRepository = funcaoDadosVersionavelRepository;
        this.dynamicExportsService = dynamicExportsService;
        this.userRepository = userRepository;
        this.userSearchRepository = userSearchRepository;
        this.equipeSearchRepository = equipeSearchRepository;
    }

    /**
     * POST /analises : Create a new analise.
     *
     * @param analise
     * the analise to create
     * @return the ResponseEntity with status 201 (Created) and with body the new
     * analise, or with status 400 (Bad Request) if the analise has already an ID
     * @throws URISyntaxException
     * if the Location URI syntax is incorrect
     */
    @PostMapping("/analises")
    @Timed
    @Secured({ROLE_ADMIN, ROLE_USER})
    public ResponseEntity<Analise> createAnalise(@Valid @RequestBody Analise analise) throws URISyntaxException {
        log.debug("REST request to save Analise : {}", analise);
        if (analise.getId() != null) {
            return ResponseEntity.badRequest().headers(
                    HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new analise cannot already have an ID")).body(null);
        }
        analise.setCreatedBy(userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).get());
        Analise analiseData = this.salvaNovaData(analise);
        linkFuncoesToAnalise(analiseData);
        Analise result = analiseRepository.save(analiseData);
        unlinkAnaliseFromFuncoes(result);
        analiseSearchRepository.save(analiseData);
        return ResponseEntity.created(new URI("/api/analises/" + result.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString())).body(result);
    }

    /**
     *
     * @param id
     * @return
     */
    private Analise recuperarAnalise(Long id) {
        return analiseRepository.findOne(id);
    }


    /**
     *
     * @return
     */
    private boolean verificarAuthority () {
        Set<Authority> listAuth = userRepository.findOneWithAuthoritiesByLogin(SecurityUtils.getCurrentUserLogin()).get().getAuthorities();
        for(Authority a : listAuth){
            if (a.getName().equals(ROLE_ADMIN)){
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param analise
     */
    private void linkFuncoesToAnalise(Analise analise) {
        linkAnaliseToFuncaoDados(analise);
        linkAnaliseToFuncaoTransacaos(analise);
    }

    /**
     *
     * @param analise
     */
    private void linkAnaliseToFuncaoDados(Analise analise) {
        analise.getFuncaoDados().forEach(funcaoDados -> {
            funcaoDados.setAnalise(analise);
            linkFuncaoDadosRelationships(funcaoDados);
            handleVersionFuncaoDados(funcaoDados, analise.getSistema());
        });
    }

    /**
     *
     * @param funcaoDados
     */
    private void linkFuncaoDadosRelationships(FuncaoDados funcaoDados) {
        funcaoDados.getFiles().forEach(file -> file.setFuncaoDados(funcaoDados));
        funcaoDados.getDers().forEach(der -> der.setFuncaoDados(funcaoDados));
        funcaoDados.getRlrs().forEach(rlr -> rlr.setFuncaoDados(funcaoDados));
    }

    /**
     *
     * @param funcaoDados
     * @param sistema
     */
    private void handleVersionFuncaoDados(FuncaoDados funcaoDados, Sistema sistema) {
        String nome = funcaoDados.getName();
        Optional<FuncaoDadosVersionavel> funcaoDadosVersionavel =
                funcaoDadosVersionavelRepository.findOneByNomeIgnoreCaseAndSistemaId(nome, sistema.getId());
        if (funcaoDadosVersionavel.isPresent()) {
            funcaoDados.setFuncaoDadosVersionavel(funcaoDadosVersionavel.get());
        } else {
            FuncaoDadosVersionavel novaFDVersionavel = new FuncaoDadosVersionavel();
            novaFDVersionavel.setNome(funcaoDados.getName());
            novaFDVersionavel.setSistema(sistema);
            FuncaoDadosVersionavel result = funcaoDadosVersionavelRepository.save(novaFDVersionavel);
            funcaoDados.setFuncaoDadosVersionavel(result);
        }
    }

    /**
     *
     * @param analise
     */
    private void linkAnaliseToFuncaoTransacaos(Analise analise) {
        analise.getFuncaoTransacaos().forEach(funcaoTransacao -> {
            funcaoTransacao.setAnalise(analise);
            funcaoTransacao.getFiles().forEach(file -> file.setFuncaoTransacao(funcaoTransacao));
            funcaoTransacao.getDers().forEach(der -> der.setFuncaoTransacao(funcaoTransacao));
            funcaoTransacao.getAlrs().forEach(alr -> alr.setFuncaoTransacao(funcaoTransacao));
        });
    }

    /**
     *
     * @param result
     */
    private void unlinkAnaliseFromFuncoes(Analise result) {
        result.getFuncaoDados().forEach(entry -> {
            entry.setAnalise(null);
        });
        result.getFuncaoTransacaos().forEach(entry -> {
            entry.setAnalise(null);
        });
    }

    /**
     * PUT /analises : Updates an existing analise.
     *
     * @param analise
     * the analise to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated
     * analise, or with status 400 (Bad Request) if the analise is not
     * valid, or with status 500 (Internal Server Error) if the analise
     * couldnt be updated
     * @throws URISyntaxException
     * if the Location URI syntax is incorrect
     */
    @PutMapping("/analises")
    @Timed
    @Secured({ROLE_ADMIN, ROLE_USER})
    public ResponseEntity<Analise> updateAnalise(@Valid @RequestBody Analise analise) throws URISyntaxException {
        log.debug("REST request to update Analise : {}", analise);
        if (analise.getId() == null) {
            return createAnalise(analise);
        } if (analise.getbloqueiaAnalise()) {
            return ResponseEntity.badRequest().headers(
                HeaderUtil.createFailureAlert(ENTITY_NAME, "analiseblocked", "You cannot edit an blocked analise")).body(null);
        }
        analise.setCreatedOn(analiseRepository.findOneById(analise.getId()).get().getCreatedOn());
        Analise analiseData = this.salvaNovaData(analise);
        linkFuncoesToAnalise(analiseData);
        Analise result = analiseRepository.save(analiseData);
        unlinkAnaliseFromFuncoes(result);
        analiseSearchRepository.save(result);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, analiseData.getId().toString()))
                .body(result);
    }

    private Analise salvaNovaData(Analise analise){
        if(analise.getDataHomologacao() != null){
            Timestamp dataDeHoje = new Timestamp(System.currentTimeMillis());
            Timestamp dataParam = analise.getDataHomologacao();
            dataParam.setHours(dataDeHoje.getHours());
            dataParam.setMinutes(dataDeHoje.getMinutes());
            dataParam.setSeconds(dataDeHoje.getSeconds());
        }
        return analise;
    }

    @PutMapping("/analises/{id}/block")
    @Timed
    @Secured({ROLE_ADMIN, ROLE_USER})
    public ResponseEntity<Analise> blockAnalise(@Valid @RequestBody Analise analise) throws URISyntaxException {
        log.debug("REST request to block Analise : {}", analise);
        if (!this.verificarAuthority()){
            return ResponseEntity.badRequest().headers(
                HeaderUtil.createFailureAlert(ENTITY_NAME, "notadmin", "Only admin users can block/unblock análises")).body(null);
        }
        linkFuncoesToAnalise(analise);
        analise.setbloqueiaAnalise(true);
        Analise result = analiseRepository.save(analise);
        unlinkAnaliseFromFuncoes(result);
        analiseSearchRepository.save(result);
        return ResponseEntity.ok().headers(HeaderUtil.blockEntityUpdateAlert(ENTITY_NAME, analise.getId().toString()))
            .body(result);
    }

    @PutMapping("/analises/{id}/unblock")
    @Timed
    @Secured({ROLE_ADMIN, ROLE_USER})
    public ResponseEntity<Analise> unblockAnalise(@Valid @RequestBody Analise analise) throws URISyntaxException {
        log.debug("REST request to block Analise : {}", analise);
        if (!this.verificarAuthority()){
            return ResponseEntity.badRequest().headers(
                HeaderUtil.createFailureAlert(ENTITY_NAME, "notadmin", "Only admin users can block/unblock análises")).body(null);
        }
        linkFuncoesToAnalise(analise);
        analise.setbloqueiaAnalise(false);
        Analise result = analiseRepository.save(analise);
        unlinkAnaliseFromFuncoes(result);
        analiseSearchRepository.save(result);
        return ResponseEntity.ok().headers(HeaderUtil.unblockEntityUpdateAlert(ENTITY_NAME, analise.getId().toString()))
            .body(result);
    }

    /**
     * GET /analises : get all the analises.
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of analises in body
     * @throws URISyntaxException
     * if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/analises")
    @Timed
    public ResponseEntity<List<Analise>> getAllAnalises(@ApiParam Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get a page of Analises");
        Page<Analise> page = analiseRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET /analises/user/:userId : get all the analises for a particular user id.
     * @param userId the user id to search for
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of analises in body
     * @throws URISyntaxException
     * if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/analises/user/{userId}")
    @Timed
    public ResponseEntity<List<Analise>> getAllAnalisesByUserId(@PathVariable Long userId, @ApiParam Pageable pageable) throws URISyntaxException {
        log.debug("REST request to get a page of Analises for user {}", userId);

        Page<Analise> page = analiseRepository.findAnaliseIdByUserId(userId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * GET /analises/:id : get the "id" analise.
     * @param id
     * the id of the analise to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the analise, or
     * with status 404 (Not Found)
     */
    @GetMapping("/analises/{id}")
    @Timed
    public ResponseEntity<Analise> getAnalise(@PathVariable Long id) {
        log.debug("REST request to get Analise : {}", id);
        Analise analise = recuperarAnalise(id);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(analise));
    }

    /**
     * DELETE /analises/:id : delete the "id" analise.
     * @param id
     * the id of the analise to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/analises/{id}")
    @Timed
    @Secured({ROLE_ADMIN, ROLE_USER})
    public ResponseEntity<Void> deleteAnalise(@PathVariable Long id) {
        log.debug("REST request to delete Analise : {}", id);
        analiseRepository.delete(id);
        analiseSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH /_search/analises?query=:query : search for the analise corresponding to the query.
     * @param query
     * the query of the analise search
     * the pagination information
     * @return the result of the search
     * @throws URISyntaxException
     * if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/_search/analises")
    @Timed
    // TODO todos os endpoint elastic poderiam ter o defaultValue impacta na paginacao do frontend
    public ResponseEntity<List<Analise>> searchAnalises(@RequestParam(defaultValue = "*") String query, @RequestParam String order, @RequestParam(name=PAGE) int pageNumber, @RequestParam int size, @RequestParam(defaultValue="id") String sort) throws URISyntaxException {
        Long idUser;
        List<Long> idEquipes, idOrganizacoes;
        Sort.Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);
        log.debug(QUERY_MSG_CONST, query);
        if (query.equals("*")) {
            log.warn("searchAnalises({}): buscando o id do usuario", query);
            idUser = this.getUserId();
            log.warn("====>> Found user_id: {}", idUser);
            if (idUser > 0) {
                // Recebendo lista de equipes do usuário
                idEquipes = userSearchRepository.findTipoEquipesById(idUser);
                log.warn("====>> Found idEquipes: {}", idEquipes);
                if (!idEquipes.isEmpty()){
                    idOrganizacoes = this.geraListaOrganizacoes(idEquipes);
                    log.warn("====>> Found idOrganizacoes: {}", idOrganizacoes);
                }
                else {
                    log.error("====>> Erro: idEquipes retoronou lista vazia.");
                }
            }
            else {
                log.error("====>> Erro: idUser não encontrado.");
            }
        }
        Page<Analise> page = analiseSearchRepository.search(queryStringQuery(query), newPageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_searchIdentificador/analises")
    @Timed
    // TODO todos os endpoint elastic poderiam ter o defaultValue impacta na paginacao do frontend
    public ResponseEntity<List<Analise>> searchIdentificadorAnalises(@RequestParam(defaultValue = "*") String query, @RequestParam String order, @RequestParam(name=PAGE) int pageNumber, @RequestParam int size, @RequestParam(defaultValue="id") String sort) throws URISyntaxException {
        log.debug(QUERY_MSG_CONST, query);
        Sort.Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);

        QueryBuilder qb = QueryBuilders.matchQuery("identificadorAnalise", query);

        Page<Analise> page = analiseSearchRepository.search((qb), newPageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_searchIdentificador/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_searchSistema/analises")
    @Timed
    // TODO todos os endpoint elastic poderiam ter o defaultValue impacta na paginacao do frontend
    public ResponseEntity<List<Analise>> searchSistemaAnalises(@RequestParam(defaultValue = "*") String query, @RequestParam String order, @RequestParam(name=PAGE) int pageNumber, @RequestParam int size, @RequestParam(defaultValue="id") String sort) throws URISyntaxException {
        log.debug(QUERY_MSG_CONST, query);
        Sort.Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);

        QueryBuilder qb = QueryBuilders.matchQuery("nomeSistema", query);

        Page<Analise> page = analiseSearchRepository.search((qb), newPageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_searchSistema/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_searchMetodoContagem/analises")
    @Timed
    // TODO todos os endpoint elastic poderiam ter o defaultValue impacta na paginacao do frontend
    public ResponseEntity<List<Analise>> searchMetodoContagemAnalises(@RequestParam(defaultValue = "*") String query, @RequestParam String order, @RequestParam(name=PAGE) int pageNumber, @RequestParam int size, @RequestParam(defaultValue="id") String sort) throws URISyntaxException {
        log.debug(QUERY_MSG_CONST, query);
        Sort.Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);

        QueryBuilder qb = QueryBuilders.matchQuery("metodoContagemString", query);

        Page<Analise> page = analiseSearchRepository.search((qb), newPageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_searchMetodoContagem/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_searchOrganizacao/analises")
    @Timed
    // TODO todos os endpoint elastic poderiam ter o defaultValue impacta na paginacao do frontend
    public ResponseEntity<List<Analise>> searchOrganizacaoAnalises(@RequestParam(defaultValue = "*") String query, @RequestParam String order, @RequestParam(name=PAGE) int pageNumber, @RequestParam int size, @RequestParam(defaultValue="id") String sort) throws URISyntaxException {
        log.debug(QUERY_MSG_CONST, query);
        Sort.Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);

        QueryBuilder qb = QueryBuilders.matchQuery("organizacao.nome", query);

        Page<Analise> page = analiseSearchRepository.search((qb), newPageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_searchOrganizacao/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    @GetMapping("/_searchEquipe/analises")
    @Timed
    // TODO todos os endpoint elastic poderiam ter o defaultValue impacta na paginacao do frontend
    public ResponseEntity<List<Analise>> searchEquipeAnalises(@RequestParam(defaultValue = "*") String query, @RequestParam String order, @RequestParam(name=PAGE) int pageNumber, @RequestParam int size, @RequestParam(defaultValue="id") String sort) throws URISyntaxException {
        log.debug(QUERY_MSG_CONST, query);
        Sort.Direction sortOrder = PageUtils.getSortDirection(order);
        Pageable newPageable = new PageRequest(pageNumber, size, sortOrder, sort);

        QueryBuilder qb = QueryBuilders.matchQuery("equipeResponsavel.nome", query);

        Page<Analise> page = analiseSearchRepository.search((qb), newPageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_searchEquipe/analises");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }


    /**
     * Função para receber o id do usuário logado
     *
     * @return id do usuário ou -1 se não encontrar usuário
     */
    private Long getUserId(){        // Pegando id do usuário logado
        String login = SecurityUtils.getCurrentUserLogin();
        User user = userRepository.findOneWithAuthoritiesByLogin(login).orElse(null);
        if (user != null) {
            return user.getId();
        }
        else {
            return Long.valueOf("-1");
        }
    }

    /**
     * Função para construir reposta do tipo Bad Request informando o erro ocorrido.
     * @param errorKey Chave de erro que será incluída na resposta
     * @param defaultMessage Mensagem padrão que será incluída no log
     * @return ResponseEntity com uma Bad Request personalizada
     */
    private ResponseEntity createBadRequest(String errorKey, String defaultMessage) {
        return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, errorKey, defaultMessage))
            .body(null);
    }

    List<Long> geraListaOrganizacoes(List<Long> idEquipes){
        int listSize = idEquipes.size();

        if (listSize > 0) {
            return this.equipeSearchRepository.findAllOrganizacaoIdById(idEquipes);
        }

        else {
            return Collections.<Long>emptyList();
        }
    }

    /**
     * Método responsável por requisitar a geração do relatório de Análise.
     * @param id
     * @throws URISyntaxException
     * @throws JRException
     * @throws IOException
     */
    @GetMapping("/relatorioPdfArquivo/{id}")
    @Timed
    public ResponseEntity<byte[]> downloadPdfArquivo(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response,this.request);
        log.debug("REST request to generate report Analise at download archive: {}", analise);
        return relatorioAnaliseRest.downloadPdfArquivo(analise, TipoRelatorio.ANALISE);
    }

    /**
     * Método responsável por requisitar a geração do relatório de Análise.
     * @throws URISyntaxException
     * @throws JRException
     * @throws IOException
     */
    @GetMapping("/relatorioPdfBrowser/{id}")
    @Timed
    public @ResponseBody byte[] downloadPdfBrowser(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response,this.request);
        log.debug("REST request to generate report Analise in browser : {}", analise);
        return relatorioAnaliseRest.downloadPdfBrowser(analise, TipoRelatorio.ANALISE);
    }

    /**
     * Método responsável por requisitar a geração do relatório de Análise.
     * @throws URISyntaxException
     * @throws JRException
     * @throws IOException
     */
    @GetMapping("/downloadPdfDetalhadoBrowser/{id}")
    @Timed
    public @ResponseBody byte[] downloadPdfDetalhadoBrowser(@PathVariable Long id) throws URISyntaxException, IOException, JRException {
        Analise analise = recuperarAnalise(id);
        relatorioAnaliseRest = new RelatorioAnaliseRest(this.response,this.request);
        log.debug("REST request to generate report Analise detalhado in browser : {}", analise);
        return relatorioAnaliseRest.downloadPdfBrowser(analise, TipoRelatorio.ANALISE_DETALHADA);
    }
    /**
     * Método responsável pela exportação da pesquisa.
     * @param tipoRelatorio
     * @throws RelatorioException
     */
    @GetMapping(value = "/analise/exportacao/{tipoRelatorio}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Timed
    public ResponseEntity<InputStreamResource> gerarRelatorioExportacao(@PathVariable String tipoRelatorio, @RequestParam(defaultValue = "*") String query) throws RelatorioException {
        ByteArrayOutputStream byteArrayOutputStream;
        try {
            new NativeSearchQueryBuilder().withQuery(multiMatchQuery(query)).build();
            Page<Analise> result =  analiseSearchRepository.search(queryStringQuery(query), dynamicExportsService.obterPageableMaximoExportacao());
            byteArrayOutputStream = dynamicExportsService.export(new RelatorioAnaliseColunas(), result, tipoRelatorio, Optional.empty(), Optional.ofNullable(AbacoUtil.REPORT_LOGO_PATH), Optional.ofNullable(AbacoUtil.getReportFooter()));
        } catch (DRException | ClassNotFoundException | JRException | NoClassDefFoundError e) {
            log.error(e.getMessage(), e);
            throw new RelatorioException(e);
        }
        return DynamicExporter.output(byteArrayOutputStream,
            "relatorio." + tipoRelatorio);
    }
}
