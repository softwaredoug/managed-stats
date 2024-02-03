# Managed Stats plugin

Managed Solr field and term statistics

This plugin lets you specify a config file to control term statistics for a field. To configure, you need to add the following

## Add this stats cache to your solrconfig.xml

This causes Solr scoring to use the global term stats contained in each field's own config file.

```
 <statsCache class="softwaredoug.solr.stats.ManagedStatsCache" />
```

## Add a special FieldType to your schema

A FieldType to configure a specific field's term statistics. This holds the overrides (CSVs). Only one may exist in your schema.

```
<fieldType name="text_general" class="softwaredoug.solr.stats.ManagedTextField" stats="text_general_stats.csv" positionIncrementGap="100">
   ...
</fieldType>
```

## The stats csv format

The CSV for the field stats, listing the field name, a term, its docFreq, totalTermFreq

```
# Contains a header with global term stats
# maxDoc, docCount, sumTotalTermFreq, sumDocFreq
#
# Followed by a term stats for this field (doc freq, total term freq)
#
# All terms will be analyzed by the field's index analyzer prior to being loaded.
#
fields
text,10,11,13,13

terms
text,foo,2,8
text,bar,1,4
text,stopword,4,5
text,UPPERCASE,3,16
text,two terms,4,24
text,comma,term,4,69
text,stemme,term,4,12
```

# Details - Analysis / tokenization

By default, the field's query analyzer will be used to process the terms in the override file.

For example, above, there is a term `stemme` in field `text`. If we were to look at the analysis chains for `text` we might see something that stems at query time, but not at index time.

```
    <field name="text" type="text_stemmed_query_only" indexed="true" stored="false" multiValued="true"/>

     ...
 
    <fieldType name="text_stemmed_query_only"  class="solr.TextField" positionIncrementGap="100">
      <analyzer type="index">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
      </analyzer>
      <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory"/>
        <filter class="solr.LowerCaseFilterFactory"/>
        <filter class="solr.PorterStemFilterFactory"/>
      </analyzer>
    </fieldType>
```

A user searches for `stemme`. Using normal Solr search processes, the search term is analyzed to the stemmed form: `stemm`.

Now Solr needs to score this match. We have a stats file available (through the above config). The managed stats plugin needs to find the now analyzed `stemm` in the file somewhere. But, alas, the line `text,stemme,term,4,12` is not a direct match.

By default, we will use `text`s query analyzer to understand how to match the files terms to incoming queries. In this case, that means that `text,stemme` is actually understood as `text,stemm`.

In the case of conflicts (multiple terms in the file being analyzed to the same value), we'll use the first one we find.


