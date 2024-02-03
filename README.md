# Managed Stats plugin - TL; DR

This plugin lets you specify a config file to control term statistics for a field. To configure, you need to add the following

## Add this stats cache to your solrconfig.xml

This causes Solr scoring to use the global term stats contained in each field's own config file.

```
 <statsCache class="softwaredoug.solr.stats.ManagedStatsCache" />
```

## Add a special FieldType to your schema

A FieldType to configure a specific field's term statistics. This holds the overrides (CSVs). Only one may exist in your schema.

```
<fieldType name="override_settings" class="softwaredoug.solr.stats.ManagedTextField" stats="text_general_stats.csv" positionIncrementGap="100">
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

# Details - Analysis / tokenization - what if we have stemmed, etc terms?

By default, the field's query analyzer will be used to process the terms in the override file.

For example, above, there is a term `stemme` in field `text`. If we were to look at Solr's analysis chains for `text` we see stemming at query time:

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

A user searches for `stemme`. Using the normal Solr search processes, the search term is analyzed to the stemmed form: `stemm`. All this happens regardless of the plugin, its just how search works. :)

Now Solr needs to score this match. Here's where the plugin matters. We have a stats file available (through the above config). The managed stats plugin needs to find the now analyzed `stemm` in the file somewhere. But, alas, the line `text,stemme,term,4,12` is not a direct match.

By default, we will use `text`s query analyzer to understand how to match the files terms to incoming queries. In this case, that means that `text,stemme` is actually understood as `text,stemm`.

In the case of conflicts (multiple terms in the file being analyzed to the same value), we'll use the first one we find.

### Changing Analysis settings

You can optionally tell this plugin which analyzer to use to understand the terms in the file in the field header, ie:

```
fields
text,10,11,13,13,raw
```

There are four options

1. `raw` - do not analyze terms in this file. Treat these terms as raw terms
2. `query` - use the field (in this case `text`'s query analyzer)
3. `index` - use the field (in this case `text`'s index analyzer)
4. `override` - use the analyzer for the override fieldType (ie `override_settings` above)

When do you choose each? It depends how you gather your stats.

When to use `raw`? - When you've done a lot of prep work. This is the ideal `raw` statistics. But this isn't always easy to acquire. It requires thinking ahead of time how query, etc terms would be analyzed in the Solr instance using the plugin.

When to use `query`? - In some cases, such as testing, you might be taking query terms, and using Solr's [terms component](https://solr.apache.org/guide/solr/latest/query-guide/terms-component.html) to get stats on the query terms. As the terms component does not perform analysis (ie `stemme`->`stemm`) you end up with lines of what your query's doc frequency, etc

When to use `index`? - Instead of using terms component on queries, you're using them on terms in documents.

When to use `override`? - Use override if you want to globally control tokenization of the file.

### Acquiring the statistics

In a testing scenario, you're downloading stats from a "true" source of the statistics. Like a production Solr instance.

Assuming you have a production Solr instance, and you want to recreate BM25 scoring on a local instance, you can use the following to gather stats on certain terms

1. Use the [terms component](https://solr.apache.org/guide/solr/latest/query-guide/terms-component.html)
2. Use various [function queries](https://solr.apache.org/guide/solr/latest/query-guide/function-queries.html) that give you stats like sumtotaltermfreq

All these take _unanalyzed terms_ (hence the analysis options above). However if you are clever, and don't mind spamming Solr, you can use the [field analysis API](https://solr.apache.org/guide/8_1/implicit-requesthandlers.html#analysis-handlers) to transform your pre-analyzed terms to analyzed terms before looking them up. Now you have "raw" terms and should specify them as such.
