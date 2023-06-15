package configs

solrUrl = "http://localhost:8983/solr/solr_system"

// todo -- refactor to bring namePatterns into localFiles (fileSystem??), add layer of 'folders'
dataSources {
    localFiles {
        MyDesktop: '/home/sean/Desktop'
        MyDocuments: '/home/sean/Documents'
        MyPictures: '/home/sean/Pictures'
    }
    email {
        // todo -- replace me, temp placeholder
        foo:'bar'
    }
    browserProfile {
        // todo -- replace me, temp placeholder
        foo:'bar'
    }
}

namePatterns.files = [
        ignore      : ~/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|robots.txt)/,
        index       : ~/(bash.*|csv|groovy|ics|ipynb|java|lst|md|php|py|rdf|rss|scala|sh|tab|te?xt|tsv)/,
        analyze     : ~/(aspx\?|cfm|docx\?|html|od.|pptx\?|pdf|ps|pub|rss|xlsx|zhtml\?)/,
        office      : ~/.*(accdb|docx?|ods|odp|odt|pdf|pptx?|rtf|txt|vsdx?|xmind|xlsx?)/,
        instructions: ~'(?i).*(adoc|readme.*|md)',
        techDev     : ~/.*(c|codeStyles|css|dat|go|gradlew|groovy|gradle|iml|ipynb|jar|java|javascript|js|map|mat|php|pyi?|sav|sbt|schema|sh|ts)/,
        config      : ~/.*(config.*|manifest.*|properties|xml|ya?ml)/,
        control     : ~/.*(ignore|license.*|lock|pem)/,
        data        : ~/.*(csv|jsonl?d?|lst|pbix|tab|tsv)/,
        media       : ~/.*(avi|jpe?g|ogg|mp3|mpe?g|png|wav)/,
        logs        : ~/.*(logs?\.?\d*)/,
        archive     : ~/.*(arc|cab|dmg|gz|jar|parquet|rar|zip|tar\.(bz2?|gz|Z)|war|zip)/,
        compressed  : ~/.*\.(bz2?|gz|z|Z)/,
        web         : ~/.*(html?)/,
        system      : ~/.*(_SUCCESS|bat|bin|bkup|cache|class|cookies|deb|gcc|lib|\.old|pkg|rpm|#$)/,
]

namePatterns.folders = [
        ignore : ~/(__snapshots__|\.bsp|\.cache|\.csv|\.gradle|\.git|\.github|\.idea|\.settings|\.svn|\.vscode|_global|ignore.*|runtime)/,
        content: ~/(content)/,
        office : ~/(?i).*(documents)/,
        techDev: ~/.*(groovy|gradle|classes)/,
        system : ~/(_global)/,
        techDev: ~/(.gradle|compile|groovy|java|main|scala|src|target|test|resources|wrapper)/,
        test   : ~/(?i).*(test)/,
        work   : ~/(?i).*(lucidworks|oconeco|work)/,
]


