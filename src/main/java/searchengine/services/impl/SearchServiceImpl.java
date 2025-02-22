package searchengine.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import searchengine.config.DataSet;
import searchengine.dto.RankDTO;
import searchengine.dto.SearchDTO;
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

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) throws IOException {
        if (offset > 0 && !DataSet.getResponse().isEmpty()) {
            List<SearchDataResponse> result = getLimitResult(DataSet.getResponse(), offset, limit);
            return new SearchResponse(true, DataSet.getResponse().size(), result);
        }
        DataSet.setResponse(new ArrayList<>());
        List<String> extractedLemmas = extractLemmas(query);
        List<LemmaEntity> foundLemmas = findLemmas(extractedLemmas, site);
        Set<SiteEntity> siteEntities = new HashSet<>();
        for (LemmaEntity lemmaEntity : foundLemmas) {
            siteEntities.add(lemmaEntity.getSiteEntityId());
        }

        List<List<SearchDataResponse>> results = new ArrayList<>();

        for (SiteEntity siteEntity : siteEntities) {
            List<LemmaEntity> extractedLemmasForOneSite = new ArrayList<>();
            for (LemmaEntity lemmaEntity : foundLemmas) {
                if (!siteEntity.getName().equals(lemmaEntity.getSiteEntityId().getName())) {
                    continue;
                }
                extractedLemmasForOneSite.add(lemmaEntity);
            }
            if (extractedLemmasForOneSite.isEmpty()) { continue; }
            if (extractedLemmas.size() != extractedLemmasForOneSite.size()) { continue; }
            List<LemmaEntity> excludedLemmasByFrequent = excludeLemmasByFrequent(extractedLemmasForOneSite);
            if (excludedLemmasByFrequent.isEmpty()) { continue; }
            List<LemmaEntity> sortedLemmasByFrequent = sortLemmasByFrequent(excludedLemmasByFrequent);
            Map<Integer, IndexEntity> indexesByLemmas = searchPagesByLemmas(sortedLemmasByFrequent);
            if (indexesByLemmas.isEmpty()) { continue; }
            Set<RankDTO> pagesRelevance = calculateRank(indexesByLemmas);
            List<RankDTO> pagesRelevanceSorted = sortPagesRelevance(pagesRelevance);
            List<SearchDataResponse> searchDataResponses
                    = convertPagesRelevanceToSearchDataResponses(sortedLemmasByFrequent, pagesRelevanceSorted);
            List<SearchDataResponse> uniqueResult = getUniqueResult(searchDataResponses);
            results.add(uniqueResult);
        }
        List<SearchDataResponse> noSortResult = results.stream()
                        .flatMap(List::stream)
                        .toList();
        List<SearchDataResponse> sortedSearchDataResponse = sortDataResponse(noSortResult);
        DataSet.setResponse(sortedSearchDataResponse);
        List<SearchDataResponse> result = getLimitResult(sortedSearchDataResponse,offset, limit);
        return new SearchResponse(true, sortedSearchDataResponse.size(), result);
    }

    private List<String> extractLemmas(String query) throws IOException {
        query = query.toLowerCase().replaceAll("ё", "е");
        return lemmaService.getLemmasFromString(query).keySet().stream().toList();
    }

    private List<LemmaEntity> findLemmas(List<String> lemmasForSearch, String site) throws IOException {
        List<LemmaEntity> lemmasFound = new ArrayList<>();
        if (site == null) {
            lemmasFound = lemmaService.findAllByLemmaIn(lemmasForSearch);
        } else {
            SiteEntity siteTarget = siteService.findByUrl(site);
            lemmasFound = lemmaService.findAllByLemmaInAndSiteEntityId(lemmasForSearch, siteTarget);
        }
        return lemmasFound;
    }

    private List<LemmaEntity> excludeLemmasByFrequent(List<LemmaEntity> lemmas) {
        lemmas.removeIf(e -> {
            boolean result = false;
            SiteEntity siteEntity = e.getSiteEntityId();
            long countPages = pageService.countBySiteEntityId(siteEntity);
            double frequency = (double) e.getFrequency();
            double count = (double) countPages - (double) countPages / 100 * 10;
            result = frequency > count;
            return result;
        });
        return lemmas;
    }

    private List<LemmaEntity> sortLemmasByFrequent(List<LemmaEntity> lemmas) {
        List<LemmaEntity> sortedLemmasToSearch = lemmas.stream().
                map(l -> new AbstractMap.SimpleEntry<>(l.getFrequency(), l)).
                sorted(Comparator.comparingInt(Map.Entry::getKey)).
                map(Map.Entry::getValue).toList();
        return sortedLemmasToSearch;
    }

    private Map<Integer, IndexEntity> searchPagesByLemmas(List<LemmaEntity> lemmas) {
        Map<Integer, IndexEntity> indexesByLemmas = indexService.findAllByLemmaId(lemmas.get(0)).stream().collect(Collectors.toMap(IndexEntity::getPageIdInt, index -> index));

        for (int i = 1; i <= lemmas.size() - 1; i++) {
            List<IndexEntity> indexNextLemma = indexService.findAllByLemmaId(lemmas.get(i));
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

    private Set<RankDTO> calculateRank(Map<Integer, IndexEntity> indexesByLemmas) {
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

        return pagesRelevance;
    }

    private List<RankDTO> sortPagesRelevance(Set<RankDTO> pagesRelevance) {
        return pagesRelevance.stream().sorted(Comparator.comparingDouble(RankDTO::getRelativeRelevance).reversed()).toList();
    }

    private List<SearchDataResponse> convertPagesRelevanceToSearchDataResponses(List<LemmaEntity> lemmasFound, List<RankDTO> pagesRelevanceSorted) throws IOException {
        List<String> simpleLemmasFromSearch = lemmasFound.stream().map(LemmaEntity::getLemma).toList();

        List<SearchDataResponse> searchDataResponses = new ArrayList<>();
        LemmaFinder lemmaFinder = LemmaFinder.getInstance();

        for (RankDTO rank : pagesRelevanceSorted) {
            Document doc = Jsoup.parse(rank.getPageEntity().getContent());
            List<String> sentences = doc.body().getElementsMatchingOwnText("[\\p{IsCyrillic}]").stream().map(Element::text).toList();
            for (String sentence : sentences) {
                StringBuilder textFromElement = new StringBuilder(sentence);
                SearchDTO searchDTO = new SearchDTO(sentence, simpleLemmasFromSearch, lemmaFinder, textFromElement);
                int searchWords = searchWords(searchDTO);
                if (searchWords < lemmasFound.size()) { continue; }
                SiteEntity sitePage = siteService.findById(pageService.findById(rank.getPageEntity()).getSiteEntityId());
                if (sitePage.getUrl().endsWith("/")) {
                    sitePage.setUrl(sitePage.getUrl().substring(0, sitePage.getUrl().length()-1));
                }
                if (textFromElement.length() > 300) {
                    textFromElement = new StringBuilder(textFromElement.substring(0));
                }
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
        return searchDataResponses;
    }

    private boolean isSetFull(List<String> simpleLemmasFromSearch, StringBuilder textFromElement, LemmaFinder lemmaFinder) throws IOException {
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(textFromElement.toString());
        Set<String> searchLemmas = new HashSet<>(simpleLemmasFromSearch);
        Set<String> sentenceLemmas = new HashSet<>(lemmas.keySet());
        for (String s : searchLemmas) {
            if(sentenceLemmas.add(s)) {
                return false;
            }
        }
        return true;
    }

    private int searchWords(SearchDTO searchDTO) throws IOException {
        String sentence = searchDTO.getSentence();
        LemmaFinder lemmaFinder = searchDTO.getLemmaFinder();
        List<String> simpleLemmasFromSearch = searchDTO.getSimpleLemmasFromSearch();
        StringBuilder textFromElement = searchDTO.getTextFromElement();
        if (!isSetFull(simpleLemmasFromSearch, textFromElement, lemmaFinder)) { return 0; }
        List<String> words = List.of(sentence.split("[ :punct]"));
        int searchWords = 0;

        for (String word : words) {
            char [] chars = word.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (!Character.isAlphabetic(chars[i])) {
                    chars[i] = '.';
                }
            }
            word = new String(chars);
            word = word.replaceAll("\\.", "");
            String lemmaFromWord = lemmaFinder.getLemmaByWord(word.replaceAll("\\p{Punct}", ""));
            if (simpleLemmasFromSearch.contains(lemmaFromWord)) {
                markWord(textFromElement, word);
                searchWords += 1;
            }
        }
        return searchWords;
    }

    private List<SearchDataResponse> getUniqueResult(List<SearchDataResponse> searchDataResponses) {
        List<SearchDataResponse> uniqueResult = new ArrayList<>();
        for (SearchDataResponse searchDataResponse : searchDataResponses) {
            boolean flag = true;
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

        for (int i = offset; i <= limit + offset; i++) {
            try {
                result.add(searchDataResponses.get(i));
            } catch (IndexOutOfBoundsException ex) {
                break;
            }
        }
        result = result.stream().sorted(Comparator.comparingInt(SearchDataResponse::getWordsFound).reversed()).toList();

        return result;
    }

    private void markWord(StringBuilder textFromElement, String word) {
        int startIndex = 0;
        while ((startIndex = textFromElement.indexOf(word, startIndex)) != -1) {
            if(startIndex != 0) {
                char[] charBefore = textFromElement.substring(startIndex - 1, startIndex).toCharArray();
                if (Character.isAlphabetic(charBefore[0])) {
                    startIndex += word.length();
                    continue;
                }
            }
            if (startIndex + word.length() + 1 < textFromElement.length()) {
                char [] charAfter = textFromElement.substring(startIndex + word.length(), startIndex + word.length() + 1).toCharArray();
                if (Character.isAlphabetic(charAfter[0])) {
                    startIndex += word.length();
                    continue;
                }
            }
            textFromElement.insert(startIndex, "<b>");
            textFromElement.insert(startIndex + word.length() + 3, "</b>");
            startIndex += word.length() + 3;
        }
    }
}