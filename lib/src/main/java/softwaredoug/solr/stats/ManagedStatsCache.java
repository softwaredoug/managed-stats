/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package softwaredoug.solr.stats;

import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.stats.LocalStatsCache;
import org.apache.solr.search.stats.LocalStatsSource;
import org.apache.solr.search.stats.StatsCache;
import org.apache.solr.search.stats.StatsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class ManagedStatsCache extends LocalStatsCache {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ManagedStatsCache() {
        super();
        log.info("Loading ManagedStatsCache");
    }

    @Override
    public StatsSource get(SolrQueryRequest req) {
        StatsSource fallback = new LocalStatsSource();
        return new ManagedStatsSource(fallback, req.getSchema());
    }

    // by returning null we don't create additional round-trip request.
    @Override
    public ShardRequest retrieveStatsRequest(ResponseBuilder rb) {
        // already incremented the stats - decrement it now
        return null;
    }

}

