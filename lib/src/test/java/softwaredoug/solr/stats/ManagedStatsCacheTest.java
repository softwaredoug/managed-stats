package softwaredoug.solr.stats;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.After;
import org.junit.Before;

public class ManagedStatsCacheTest extends SolrTestCaseJ4 {

    private ManagedTextField managedField;

    @Before
    public void setupStats() throws Exception {
        // All tests relative to this
        initCore("solrconfig.xml", "schema.xml", "build/resources/test/solr");

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

    public void testSearchNotManagedTerm() {
        assertQ(
                "searching for not managed term",
                req(
                        "q", "tool",
                        "qf", "text",
                        "defType", "edismax"),
                "*[count(//doc)=1]");
    }

    public void testSearchManagedTermMatchesOne() {
        assertQ(
                "dumb search",
                req(
                        "q", "foo",
                        "qf", "text",
                        "defType", "edismax"),
                "*[count(//doc)=1]");
    }

    public void testSearchManagedTermUsesCorrectDocFreq() {
        assertQ(
                "search uses managed doc freq",
                req(
                        "q", "foo",
                        "qf", "text",
                        "defType", "edismax",
                        "debug", "true"),
                "//lst[@name='explain']/str[@name='3' and contains(text(),\"2 = n, number of documents containing term\")]");
    }

    public void testSearchManagedTermUsesCorrectDocCount() {
        assertQ(
                "search uses doc count",
                req(
                        "q", "foo",
                        "qf", "text",
                        "defType", "edismax",
                        "debug", "true"),
                "//lst[@name='explain']/str[@name='3' and contains(text(),\"10 = N, total number of documents with field\")]");
    }

    public void testSearchNonManagedField() {
        assertQ(
                "search uses doc count",
                req(
                        "q", "burritos",
                        "qf", "not_managed",
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
