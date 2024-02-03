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
text,stopword,4,8
text,UPPERCASE,3,8
text,two terms,4,8
text,comma,term,4,8
```
