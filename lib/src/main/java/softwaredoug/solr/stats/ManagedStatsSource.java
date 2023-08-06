package softwaredoug.solr.stats;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.stats.LocalStatsSource;
import org.apache.solr.search.stats.StatsCache;
import org.apache.solr.search.stats.StatsSource;

import java.io.IOException;

public class ManagedStatsSource extends StatsSource {

    private StatsSource fallback;

    private IndexSchema schema;

    public ManagedStatsSource(StatsSource fallback, IndexSchema schema) {
        this.fallback = fallback;
        this.schema = schema;
    }

    private ManagedTextField getAsManagedTextField(String field) {
        SchemaField scheamField = this.schema.getField(field);
        ManagedTextField fieldType = (ManagedTextField)scheamField.getType();
        return fieldType;
    }

    @Override
    public TermStatistics termStatistics(SolrIndexSearcher localSearcher, Term term, TermContext context) throws IOException {
        ManagedTextField fieldType = this.getAsManagedTextField(term.field());
        if (fieldType == null) {
            return this.fallback.termStatistics(localSearcher, term, context);
        }
        TermStatistics termStats = fieldType.termStatistics(term);
        if (termStats == null) {
            termStats = this.fallback.termStatistics(localSearcher, term, context);
        }
        return termStats;    }

    @Override
    public CollectionStatistics collectionStatistics(SolrIndexSearcher localSearcher, String field) throws IOException {
        ManagedTextField fieldType = this.getAsManagedTextField(field);
        if (fieldType == null) {
            return this.fallback.collectionStatistics(localSearcher, field);
        }
        return fieldType.collectionStatistics(field);
    }
}
