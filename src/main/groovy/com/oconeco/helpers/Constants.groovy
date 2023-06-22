package com.oconeco.helpers

import com.oconeco.analysis.BaseAnalyzer

import java.util.regex.Pattern

class Constants {

    public static final String LBL_ARCHIVE = 'archive'
    public static final String LBL_CRAWL = 'crawl'
    public static final String LBL_IGNORE = 'ignore'
    public static final String LBL_UNKNOWN = 'unknown'

    public static final Map<String, Pattern> DEFAULT_FOLDERNAME_PATTERNS = [
            ignore       : ~/.*(?i)(csv|\.gradle.?|\.m2|snapshots?|caches?|chrome|deleteme.*|flatpack|git|github|google-chrome|ignore.*|never_index|node_modules|packages?|Partitions|pkgs?|plugins?|repo|repository|skins?|skipme.*|svn|target|te?mp|vscode)/,
            backups      : ~/.*(?i)(backups?|bkups?|old|timeshift)/,
            configuration: ~/.*(?i)(configs)/,
            documents    : ~/.*([Dd]ocuments|[Dd]esktop)/,
            downloads    : ~/.*([Dd]ownloads)/,
            pictures     : ~/.*([Pp]ictures)/,
            programming  : ~/.*(?i)(gradle|groovy|java|jupyter|resources|src|work)/,
            communication: ~/.*(?i)(mail)/,
            system       : ~/.*(?i)(class|sbt|sbt-\d.*|runtime)/,
    ]
    public static final Map<String, Map<String, Object>> DEFAULT_FOLDERNAME_LOCATE = [
            ignore : [pattern: Constants.DEFAULT_FOLDERNAME_PATTERNS[Constants.LBL_IGNORE], analysis: [com.oconeco.analysis.BaseAnalyzer.IGNORE]],
            default: [pattern: null, analysis: com.oconeco.analysis.BaseAnalyzer.DEFAULT],
    ]


    public static final Map<String, Pattern> DEFAULT_FILENAME_PATTERNS = [
            ignore        : ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/,
            office        : ~/.*\.(?i)(accdb|docx?|ods|odp|odt|pptx?|rtf|vsdx?|xlsx?)/,
            system        : ~/.*(\.(?i)(bin|deb|lib|pkg|rpm)|(gcc.*))/,
            archive       : ~/.*\.(?i)(arc|gz|rar|zip|tar.gz|zip)/,
            web           : ~/.*(?i)(html?)/,
            instructions  : ~'(?i).*(adoc|readme.*|md)',
            techDev       : ~/.*(?i)(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
            config        : ~/.*(?i)(cfg|config.*|pem|properties|xml|yaml)/,
            data          : ~/.*(?i)(csv|jsonl?d?|lst|tab)/,
            media         : ~/.*(?i)(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
            communication : ~/.*(?i)(eml|vcf)/,
    ]

    public static final Map<String, Map<String, Object>> DEFAULT_FILENAME_LOCATE = [
            ignore : [pattern:  ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: BaseAnalyzer.IGNORE],
            basic : [pattern:  ~/.*(?i)(accdb|docx?|go|groovy|gradle|jar|java|javascript|js|jsonl?d?|md|ods|odp|odt|php|pptx?|rtf|schema|sh|vsdx?|xlsx?)/, analysis: BaseAnalyzer.BASIC_LIST],
            default: [pattern: null, analysis: BaseAnalyzer.DEFAULT],
    ]


    public static final Map<String, Pattern> DEFAULT_FOLDERPATH_PATTERNS = [
            configuration: ~/.*(?i)(\/.local\/|configs)/,
            data         : ~/.*(\/data\/).*/,
            build           : ~/.*(\/build\/).*/,
            work           : ~/.*\/work\/.*/,
    ]

    public static final Map<String, Pattern> DEFAULT_FILEPATH_PATTERNS = [
//            configuration: ~/.*(?i)(\/.local\/|configs)/,
//            data         : ~/.*(\/data\/).*/,
//            build           : ~/.*(\/build\/).*/,
            work           : ~/.*\/work\/.*/,
    ]


}
