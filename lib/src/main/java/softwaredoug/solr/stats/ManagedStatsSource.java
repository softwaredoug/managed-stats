package softwaredoug.solr.stats;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.stats.LocalStatsSource;
import org.apache.solr.search.stats.StatsCache;
import org.apache.solr.search.stats.StatsSource;

import java.io.IOException;

public class ManagedStatsSource extends StatsSource {

    private final StatsCache.StatsCacheMetrics metrics;
    private StatsSource fallback;

    public ManagedStatsSource(StatsSource fallback, StatsCache.StatsCacheMetrics metrics) {
        this.fallback = fallback;
        this.metrics = metrics;
    }

    @Override
    public TermStatistics termStatistics(SolrIndexSearcher localSearcher, Term term, int docFreq, long totalTermFreq) throws IOException {
        return new TermStatistics(term.bytes(),1,1);
    }

    @Override
    public CollectionStatistics collectionStatistics(SolrIndexSearcher localSearcher, String field) throws IOException {
        return new CollectionStatistics(field, 100, 100,
                        100, 100);
    }
}
