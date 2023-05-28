package com.oconeco.helpers

import java.util.regex.Pattern

class Constants {

    public static final String LBL_ARCHIVE = 'archive'
    public static final String LBL_CRAWL = 'crawl'
    public static final String LBL_IGNORE = 'ignore'
    public static final String LBL_UNKNOWN = 'unknown'

    public static final Map<String, Pattern> DEFAULT_FOLDERNAME_PATTERNS = [
            ignore       : ~/.*(\.gradle|\.m2|\b*snapshots?\b*|\b*caches?\b*|git|github|ignore.*|node_modules|packages?|pkgs?|plugins?|repository|skins?|skipme.*|svn|target|tmp|vscode)/,
            backups      : ~/.*(backups?|bkups?|old|timeshift)/,
            configuration: ~/.*(configs)/,
            documents    : ~/.*([Dd]ocuments|[Dd]esktop)/,
            downloads    : ~/.*([Dd]ownloads)/,
            pictures     : ~/.*([Pp]ictures)/,
            programming  : ~/.*(java|jupyter|src|work)/,
            system       : ~/.*(class|sbt|sbt-\d.*|runtime)/,
    ]

    public static final Map<String, Pattern> DEFAULT_FILENAME_PATTERNS = [
            ignore      : ~/([.~]*lock.*|_.*|.*\.te?mp$|.*\.class$|.*\.pem|robots.txt|skipme.*|.*\.tmp)/,
            office      : ~/.*\.(accdb|docx?|ods|odp|odt|pptx?|rtf|vsdx?|xlsx?)/,
            system      : ~/.*(\.(bin|deb|lib|pkg|rpm)|(gcc.*))/,
            archive     : ~/.*\.(arc|gz|rar|zip|tar.gz|zip)/,
            web         : ~/.*(html?)/,
            instructions: ~'(?i).*(adoc|readme.*|md)',
            techDev     : ~/.*(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
            config      : ~/.*(cfg|config.*|pem|properties|xml|yaml)/,
            data        : ~/.*(csv|jsonl?d?|lst|tab)/,
            media       : ~/.*(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
    ]
    public static int DEFAULT_FOLDER_SIZE = 4096     // todo -- revisit, this is just an arbitrary starting size...?

//    public static final String TOCRAWL = 'crawl'
//    public static final String TOIGNORE = 'ignore'
}
