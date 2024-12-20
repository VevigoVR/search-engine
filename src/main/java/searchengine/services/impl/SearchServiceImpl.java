package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.RankDTO;
import searchengine.dto.StatusSite;
import searchengine.dto.entity.IndexEntity;
import searchengine.dto.entity.LemmaEntity;
import searchengine.dto.response.SearchDataResponse;
import searchengine.dto.entity.SiteEntity;
import searchengine.dto.response.SearchResponse;
import searchengine.services.IndexService;
import searchengine.services.PageService;
import searchengine.services.SearchService;
import searchengine.services.SiteService;
import searchengine.services.LemmaService;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final StatusSite indexSuccessStatus = StatusSite.INDEXED;

    private List<LemmaEntity> exclusionLemmasByFrequent(String query, String site) throws IOException {
        SiteEntity siteTarget = siteService.findByUrl(site);
        long countPages;
        List<LemmaEntity> lemmasFound = new ArrayList<>();
        query = query.toLowerCase().replaceAll("ё", "е");
        List<String> lemmasForSearch = lemmaService.getLemmasFromString(query).keySet().stream().toList();

        if (siteTarget != null) {
            countPages = pageService.countBySiteEntityId(siteTarget);
            lemmasFound = lemmaService.findAllByLemmaInAndSiteEntityId(lemmasForSearch, siteTarget);
            System.out.println("lemmasFound size before remove: " + lemmasFound.size());

            lemmasFound.removeIf(e -> {
                log.info("Lemma frequency(" + e + "):" + e.getFrequency());
                log.info("Frequency limit:" + (double) countPages / Math.PI);
                System.out.println("return: " + ((double) e.getFrequency() > ((double) countPages / Math.PI)));
                if (countPages < 100) {
                    return false;
                }
                return ((double) e.getFrequency() > ((double) countPages / Math.PI));
            });
        } else {
            lemmasFound = lemmaService.findAllByLemmaIn(lemmasForSearch);
            System.out.println("lemmasFound size before remove: " + lemmasFound.size());
            lemmasFound.removeIf(e -> {
                SiteEntity siteEntity = e.getSiteEntityId();
                log.info("Lemma frequency(" + e + "):" + e.getFrequency());
                log.info("Frequency limit:" + (double) pageService.countBySiteEntityId(siteEntity) / Math.PI);
                System.out.println("return: " + ((double) e.getFrequency() > ((double) pageService.countBySiteEntityId(siteEntity) / Math.PI)));
                if (pageService.countBySiteEntityId(siteEntity) < 100) {
                    return false;
                }
                return ((double) e.getFrequency() > ((double) pageService.countBySiteEntityId(siteEntity) / Math.PI));
            });
        }

        return lemmasFound;
    }

    private List<LemmaEntity> sortingLemmasByFrequent(List<LemmaEntity> lemmas) {
        List<LemmaEntity> sortedLemmasToSearch = lemmas.stream().
                map(l -> new AbstractMap.SimpleEntry<>(l.getFrequency(), l)).
                sorted(Comparator.comparingInt(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();
        //System.out.println("Sorted lemmas: ");
        sortedLemmasToSearch.forEach(l -> System.out.println(l.getLemma() + " - " + l.getFrequency()));
        return sortedLemmasToSearch;
    }

    private Map<Integer, IndexEntity> searchPagesByFirstLemma(List<LemmaEntity> lemmas) {
        Map<Integer, IndexEntity> indexesByLemmas = indexService.findIndexesByLemma(lemmas.get(0)).stream().collect(Collectors.toMap(IndexEntity::getPageIdInt, index -> index));
        //System.out.println("Карта лемм: ");
        for (Integer page : indexesByLemmas.keySet()) {
            //System.out.println("page id: " + page);
            //System.out.println("lemma: " + indexesByLemmas.get(page).getLemmaId().getLemma() + " - " + indexesByLemmas.get(page).getRank());
        }

        for (int i = 1; i <= lemmas.size() - 1; i++) {
            List<IndexEntity> indexNextLemma = indexService.findIndexesByLemma(lemmas.get(i));
            List<Integer> pagesToSave = new ArrayList<>();
            for (IndexEntity indexNext : indexNextLemma) {
                if (indexesByLemmas.containsKey(indexNext.getPageId().getId())) {
                    pagesToSave.add(indexNext.getPageId().getId());
                }
            }
            indexesByLemmas.entrySet().removeIf(entry -> !pagesToSave.contains(entry.getKey()));
        }
        return indexesByLemmas;
    }

    private Set<RankDTO> rankCalculation(Map<Integer, IndexEntity> indexesByLemmas) {
        Set<RankDTO> pagesRelevance = new HashSet<>();
        int pageId = indexesByLemmas.values().stream().toList().get(0).getPageId().getId();
        RankDTO rankPage = new RankDTO();
        for (IndexEntity index : indexesByLemmas.values()) {
            if (index.getPageId().getId() == pageId) {
                rankPage.setPageEntity(index.getPageId());
            } else {
                rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
                pagesRelevance.add(rankPage);
                rankPage = new RankDTO();
                rankPage.setPageEntity(index.getPageId());
                pageId = index.getPageId().getId();
            }
            rankPage.setPageId(index.getPageId().getId());
            rankPage.setAbsRelevance(rankPage.getAbsRelevance() + index.getRank());
            if (rankPage.getMaxLemmaRank() < index.getRank()) rankPage.setMaxLemmaRank(index.getRank().intValue());
        }
        rankPage.setRelativeRelevance(rankPage.getAbsRelevance() / rankPage.getMaxLemmaRank());
        pagesRelevance.add(rankPage);

        System.out.println("pagesRelevance.size(): " + pagesRelevance.size());
        return pagesRelevance;
    }

    private List<RankDTO> sortPagesRelevance(Set<RankDTO> pagesRelevance) {
        return pagesRelevance.stream().sorted(Comparator.comparingDouble(RankDTO::getRelativeRelevance).reversed()).toList();
    }

    private List<SearchDataResponse> convertingPagesRelevanceToSearchDataResponses(List<LemmaEntity> lemmasFound, List<RankDTO> pagesRelevanceSorted) throws IOException {

        List<String> simpleLemmasFromSearch = lemmasFound.stream().map(LemmaEntity::getLemma).toList();
        /*System.out.println("simpleLemmasFromSearch size in method: " + simpleLemmasFromSearch.size());
        for (String lemma : simpleLemmasFromSearch) {
            System.out.println("lemma: " + lemma);
        }

         */
        List<SearchDataResponse> searchDataResponses = new ArrayList<>();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();

        for (RankDTO rank : pagesRelevanceSorted) {
            Document doc = Jsoup.parse(rank.getPageEntity().getContent());
            List<String> sentences = doc.body().getElementsMatchingOwnText("[\\p{IsCyrillic}]").stream().map(Element::text).toList();
            //System.out.println("sentences: " + sentences.size() + ", isEmpty(): " + sentences.isEmpty());
            for (String sentence : sentences) {
                //System.out.println("sentence: " + sentence);
                StringBuilder textFromElement = new StringBuilder(sentence);
                List<String> words = List.of(sentence.split("[\s:punct]"));
                int searchWords = 0;
                for (String word : words) {
                    word = word.replaceAll("«", "");
                    word = word.replaceAll("»", "");
                    String lemmaFromWord = lemmaFinder.getLemmaByWord(word.replaceAll("\\p{Punct}", ""));
                    //System.out.println("lemmaFromWord: " + lemmaFromWord + ", word = " + word);
                    if (simpleLemmasFromSearch.contains(lemmaFromWord)) {
                        markWord(textFromElement, word, 0);
                        searchWords += 1;
                    }
                }
                //System.out.println("searchWords: " + searchWords);
                if (searchWords != 0) {
                    //log.info("до получения сайта");
                    SiteEntity sitePage = siteService.findById(pageService.findById(rank.getPageEntity()).getSiteEntityId());
                    if (sitePage.getUrl().endsWith("/")) {
                        sitePage.setUrl(sitePage.getUrl().substring(0, sitePage.getUrl().length()-1));
                    }
                    //log.info("после получения сайта");
                    System.out.println("sitePage.getUrl(): " + sitePage.getUrl() + ", "
                    + "sitePage.getName(): " + sitePage.getName() + ", "
                    + "rank.getPageEntity().getPath(): " + rank.getPageEntity().getPath());
                    searchDataResponses.add(new SearchDataResponse(
                            sitePage.getUrl(),
                            sitePage.getName(),
                            rank.getPageEntity().getPath(),
                            doc.title(),
                            textFromElement.toString(),
                            rank.getRelativeRelevance(),
                            searchWords
                    ));
                }
            }
        }
        return searchDataResponses;
    }

    private List<SearchDataResponse> getUniqueResult(List<SearchDataResponse> searchDataResponses) {
        List<SearchDataResponse> uniqueResult = new ArrayList<>();
        for (SearchDataResponse searchDataResponse : searchDataResponses) {
            boolean flag = true;
            System.out.println("searchDataResponse url: " + searchDataResponse.getUri());
            for (SearchDataResponse unique : uniqueResult) {
                if (unique.getUri().contains(searchDataResponse.getUri())) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                uniqueResult.add(searchDataResponse);
            }

        }
        return uniqueResult;
    }

    private List<SearchDataResponse> sortDataResponse(List<SearchDataResponse> searchDataResponses) {
        return searchDataResponses
                .stream()
                .sorted(Comparator
                        .comparingDouble(SearchDataResponse::getRelevance)
                        .reversed())
                .toList();
    }

    private List<SearchDataResponse> getLimitResult(List<SearchDataResponse> searchDataResponses, int offset, int limit) {
        List<SearchDataResponse> result = new ArrayList<>();
        for (int i = limit * offset; i <= limit * offset + limit; i++) {
            try {
                result.add(searchDataResponses.get(i));
            } catch (IndexOutOfBoundsException ex) {
                break;
            }
        }
        result = result.stream().sorted(Comparator.comparingInt(SearchDataResponse::getWordsFound).reversed()).toList();
        System.out.println("result.size(): " + result.size());
        System.out.println("result: ");
        for (SearchDataResponse data : result) {
            System.out.println(data.getSiteName());
            System.out.println(data.getRelevance());
            System.out.println(data.getUri());
            System.out.println(data.getTitle());
            System.out.println(data.getSite());
            System.out.println(data.getWordsFound());
            System.out.println(data.getSnippet());
            System.out.println();
        }
        return result;
    }

    @Override
    public ResponseEntity<Object> search(String query, String site, int offset, int limit) throws IOException {
        // Exclusion lemmas by frequent
        List<LemmaEntity> lemmasFound = exclusionLemmasByFrequent(query, site);
        System.out.println("lemmasFound size: " + lemmasFound.size());
        if (lemmasFound.isEmpty()) {
            System.out.println("Возврат пустого объекта");
            return ResponseEntity.ok(new SearchResponse(true, 0, Collections.emptyList()));
        }
        //Sorting lemmas by frequent
        List<LemmaEntity> sortedLemmasToSearch = sortingLemmasByFrequent(lemmasFound);
        System.out.println("sortedLemmasToSearch size: " + sortedLemmasToSearch.size());
        //Search pages by first lemma
        Map<Integer, IndexEntity> indexesByLemmas = searchPagesByFirstLemma(sortedLemmasToSearch);
        System.out.println("indexesByLemmas size: " + indexesByLemmas.size());
        //Output if empty result
        if (indexesByLemmas.isEmpty()) {
            System.out.println("indexesByLemmas.isEmpty(): " + true);
            return ResponseEntity.ok().body(new SearchResponse(true, 0, Collections.emptyList()));
        }
        //Rank calculation
        Set<RankDTO> pagesRelevance = rankCalculation(indexesByLemmas);
        System.out.println("pagesRelevance size: " + pagesRelevance.size());
        //Sort pages Relevance
        List<RankDTO> pagesRelevanceSorted = sortPagesRelevance(pagesRelevance);
        System.out.println("pagesRelevanceSorted size: " + pagesRelevanceSorted.size());

        //Converting pages relevance to searchDataResponses
        List<SearchDataResponse> searchDataResponses
                = convertingPagesRelevanceToSearchDataResponses(lemmasFound, pagesRelevanceSorted);
        System.out.println("searchDataResponses size: " + searchDataResponses.size());
        List<SearchDataResponse> uniqueResult = getUniqueResult(searchDataResponses);
        System.out.println("uniqueResult size: " + uniqueResult.size());
        List<SearchDataResponse> sortedSearchDataResponse = sortDataResponse(uniqueResult);
        System.out.println("sortedSearchDataResponse size: " + sortedSearchDataResponse.size());
        List<SearchDataResponse> result = getLimitResult(sortedSearchDataResponse,offset, limit);
        System.out.println("result size: " + result.size());

        return ResponseEntity.ok(new SearchResponse(true, result.size(), result));
    }

    private void markWord(StringBuilder textFromElement, String word, int startPosition) {
        int start = textFromElement.indexOf(word, startPosition);
        if (textFromElement.indexOf("<b>", start - 3) == (start - 3)) {
            markWord(textFromElement, word, start + word.length());
            return;
        }
        int end = start + word.length();
        textFromElement.insert(start, "<b>");
        if (end == -1) {
            textFromElement.insert(textFromElement.length(), "</b>");
        } else textFromElement.insert(end + 3, "</b>");
    }
}
