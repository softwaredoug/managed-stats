package softwaredoug.solr.stats;

import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.rest.ManagedResource;
import org.apache.solr.rest.ManagedResourceObserver;
import org.apache.solr.schema.TextField;

import java.io.IOException;

public class ManagedTextField extends TextField implements ResourceLoaderAware {

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        final SolrResourceLoader solrResourceLoader = (SolrResourceLoader) loader;
        // ((SolrResourceLoader) loader).openResource("");
    }

}
