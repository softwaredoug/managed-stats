package softwaredoug.solr.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.junit.After;
import org.junit.Before;

public class ManagedStatsChooseAnalyzerTest extends SolrTestCaseJ4 {
    private ManagedTextField managedField;

    @Before
    public void setupStats() throws Exception {
        // All tests relative to this
        initCore("solrconfig.xml", "schema_override_analysis_control.xml", "build/resources/test/solr");

        SolrCore core = this.h.getCore();
        IndexSchema schema = core.getLatestSchema();
        FieldType fieldType = schema.getFieldTypeByName("text_general");
        this.managedField = (ManagedTextField)fieldType;

        indexDocs();
    }

    @After
    public void cleanup() {
        deleteCore();
    }

    public void testSearchManagedUsesOverrideFieldsAnalyzer() {
        /* Check for these overrides:
        text_stem,cats,3,4
        text_stem,policy,5,9
         */
        assertQ(
                "search uses doc count based on override managed analyzer",
                req(
                        "q", "cat",
                        "qf", "text_stem_query_only",
                        "defType", "edismax",
                        "debug", "true"),
                "//lst[@name='explain']/str[@name='4' and contains(text(),\"3 = n, number of documents containing term\")]");

        assertQ(
                "search uses doc count based on override managed analyzer",
                req(
                        "q", "cats",
                        "qf", "text_stem_query_only",
                        "defType", "edismax",
                        "debug", "true"),
                "//lst[@name='explain']/str[@name='4' and contains(text(),\"3 = n, number of documents containing term\")]");
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
        assertU(adoc("id", "4", "text", "policy notlike cat cats", "not_managed", "nachos"
        ));
        assertU(commit());
    }

}
