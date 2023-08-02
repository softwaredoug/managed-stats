# Managed Stats plugin (experimental)

Managed Solr field and term statistics

This plugin lets you specify a config file to control term statistics for a field. It contains

## A custom stats cache

This causes Solr scoring to use the global term stats contained in each field's own config file.

```
 <statsCache class="softwaredoug.solr.stats.ManagedStatsCache" />
```

## A custom FieldType

A FieldType to configure a specific field's term statistics. It functions exactly like a TextField, however it adds `stats` which contains a file for field statistics.

```
<fieldType name="text_general" class="softwaredoug.solr.stats.ManagedTextField" stats="text_general_stats.csv" positionIncrementGap="100">
   ...
</fieldType>
```

## The stats csv format

The CSV for the field stats.

```
# Contains a header with global term stats
# maxDoc, docCount, sumTotalTermFreq, sumDocFreq
#
# Followed by a term stats for this field (doc freq, total term freq)
#
# All terms will be analyzed by the field's index analyzer prior to being loaded.
#
10,11,13,13
foo,2,8
bar,1,4
stopword,4,8
UPPERCASE,3,8
two terms,4,8
```
