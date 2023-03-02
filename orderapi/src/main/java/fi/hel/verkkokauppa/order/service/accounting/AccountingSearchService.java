package fi.hel.verkkokauppa.order.service.accounting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.hel.verkkokauppa.order.mapper.AccountingExportDataMapper;
import fi.hel.verkkokauppa.order.model.Order;
import fi.hel.verkkokauppa.order.model.accounting.AccountingExportData;
import fi.hel.verkkokauppa.order.service.elasticsearch.SearchAfterService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccountingSearchService {

    @Autowired
    private ElasticsearchOperations operations;
    @Autowired
    private SearchAfterService searchAfterService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<AccountingExportData> getNotExportedAccountingExportData() throws Exception {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("exported"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb).build();

        SearchRequest searchRequest = searchAfterService.buildSearchAfterSearchRequest(
                "accountingexportdatas",
                query,
                new FieldSortBuilder("_id").order(SortOrder.DESC), // cannot sort with text field (accountingSlipId) so using _id
                new FieldSortBuilder("timestamp").order(SortOrder.DESC));

        log.info(searchRequest.toString());
        org.elasticsearch.search.SearchHit[] hits = searchAfterService.executeSearchRequest(searchRequest);

        final List<AccountingExportData> exportData = Arrays.stream(hits).map(org.elasticsearch.search.SearchHit::getSourceAsString).map(s -> {
            try {
                return objectMapper.readValue(s,AccountingExportData.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        log.info(objectMapper.writeValueAsString(exportData));



//        static <T> T[] concatWithStream(T[] array1, T[] array2) {
//            return Stream.concat(Arrays.stream(array1), Arrays.stream(array2))
//                    .toArray(size -> (T[]) Array.newInstance(array1.getClass().getComponentType(), size));
//        }


//        SearchHits<AccountingExportData> hits = operations.search(queryOld, AccountingExportData.class);

//        SearchPage<AccountingExportData> searchHits = SearchHitSupport.searchPageFor(hits, query.getPageable());
//
//        final List<AccountingExportData> exportData = searchHits.stream()
//                .map(SearchHit::getContent)
//                .collect(Collectors.toList());
//        exportData.addAll(exportData);

        if (exportData.isEmpty()) {
            return new ArrayList<>();
        }
        return exportData;
    }

    public List<Order> findNotAccounted() {
        BoolQueryBuilder qb = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("accounted"));

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withPageable(PageRequest.of(0, 10000))
                .build();
        SearchHits<Order> hits = operations.search(query, Order.class);

        SearchPage<Order> searchHits = SearchHitSupport.searchPageFor(hits, query.getPageable());

        final List<Order> exportData = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        if (exportData.isEmpty()) {
            return new ArrayList<>();
        }

        return exportData;
    }

}
