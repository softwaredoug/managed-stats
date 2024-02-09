package softwaredoug.solr.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.junit.Before;

public class ManagedStatsCacheNoFileTest extends SolrTestCaseJ4 {

    private ManagedTextField managedField;

    @Before
    public void setupStats() throws Exception {
        // All tests relative to this
        initCore("solrconfig.xml", "schema_override_file_missing.xml", "build/resources/test/solr");

        SolrCore core = this.h.getCore();
        IndexSchema schema = core.getLatestSchema();
        FieldType fieldType = schema.getFieldTypeByName("text_general");
        this.managedField = (ManagedTextField)fieldType;

        indexDocs();
    }

    public void testSearchWithNoOverridesFileWorks() {
        assertQ(
                "search uses doc count",
                req(
                        "q", "burritos",
                        "qf", "not_managed",
                        "defType", "edismax",
                        "debug", "true"),
                "*[count(//doc)=1]");

        assertQ(
                "search uses doc count",
                req(
                        "q", "planets",
                        "qf", "text",
                        "defType", "edismax",
                        "debug", "true"),
                "*[count(//doc)=1]");
    }

    public void indexDocs() {

        assertU(adoc("id", "1",
                "text", "Democratic Order op Planets"));
        assertU(adoc("id", "2", "text", "Tool", "not_managed", "tacos"
        ));
        assertU(adoc("id", "3", "text", "foo", "not_managed", "burritos"
        ));
        assertU(adoc("id", "4", "text", "bar", "not_managed", "nachos"
        ));
        assertU(commit());
    }

}
