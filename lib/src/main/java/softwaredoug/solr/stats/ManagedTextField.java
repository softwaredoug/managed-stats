package softwaredoug.solr.stats;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;

import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.core.SolrResourceNotFoundException;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.schema.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class ManagedTextField extends TextField implements ResourceLoaderAware {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String statsFile;

    private Overrides overrides;


    public static Overrides.AnalysisOption DEFAULT_ANALYSIS = Overrides.AnalysisOption.QUERY;

    @Override
    protected void init(IndexSchema schema, Map<String, String> args) {
        this.statsFile = args.remove("stats");
        this.overrides = null;

        String override = args.remove("override");
        if (override != null) {
            log.warn("Override setting is deprecated and considered to always be enabled. Ignoring");
        }

        super.init(schema, args);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        final SolrResourceLoader solrResourceLoader = (SolrResourceLoader) loader;
        String typeName = getTypeName();
        List<String> lines = new ArrayList<>();
        try {
            lines = solrResourceLoader.getLines(this.statsFile);
        }
        catch (SolrResourceNotFoundException e) {
            log.warn("File {} not found, ManagedStats plugin will default to local stats");
        }
        Map<String, String> config = getNonFieldPropertyArgs();
        this.overrides = new Overrides(lines, typeName, config);
    }

    public TermStatistics termStatistics(Term term, Map<Overrides.AnalysisOption, Analyzer> analyzerOptions) {
        // slow for correctness, very bad to do this here
        log.trace("Lookup stats for term: {}", term);
        return this.overrides.findBestOverride(term, analyzerOptions);
    }

    public CollectionStatistics collectionStatistics(String field) {
        log.trace("Lookup stats for field: {} fieldStats len: {}", field, this.overrides.getFieldStats().size());
        return this.overrides.getFieldStats().get(field);
    }

    @Override
    public IndexableField createField(SchemaField field, Object value) {
        return super.createField(field, value);
    }

}
