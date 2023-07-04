package com.oconeco.helpers

import java.util.regex.Pattern

class Constants {

    public static final String LBL_ARCHIVE = 'archive'
    public static final String LBL_CRAWL = 'crawl'
    public static final String LBL_IGNORE = 'ignore'
    public static final String LBL_UNKNOWN = 'unknown'

    public static final String IGNORE = 'ignore'
    public static final List<String> IGNORE_BUNDLE = [IGNORE]
    public static final String DEFAULT = 'default'
    public static final String TRACK = 'track'
    public static final List<String> TRACK_BUNDLE = [TRACK]
    public static final String PARSE = 'parse'
//    public static final String BASIC = 'basic'
    public static final List<String> PARSE_BUNDLE = [TRACK, PARSE]

    public static final String SOURCE_CODE = 'sourceCode'
    public static final List<String> SOURCE_BUNDLE = [TRACK, SOURCE_CODE]
    public static final String LOGS = 'logs'
    public static final List<String> LOGS_BUNDLE = [TRACK, LOGS]

    public static final String ARCHIVE = 'archive'
    public static final List<String> ARCHIVE_BUNDLE = [ARCHIVE, TRACK]          // currently handling ARCHIVE in crawler, and processing each archive file as a separate group (to avoid OOME...?)

    public static final String LABEL_MATCH = 'LabelMatch'
    public static final String NO_MATCH = 'no match'

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
            (IGNORE) : [pattern: DEFAULT_FOLDERNAME_PATTERNS[LBL_IGNORE], analysis: TRACK_BUNDLE],          // track ignored folders, but do not descend/crawl
            'test' : [pattern: ~/(test.*)/, analysis: TRACK_BUNDLE],
            (TRACK): [pattern: null, analysis: TRACK_BUNDLE],
    ]

    public static final Map<String, Pattern> DEFAULT_FILENAME_PATTERNS = [
            ignore       : ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/,
            office       : ~/.*\.(?i)(accdb|docx?|ods|odp|odt|pptx?|rtf|vsdx?|xlsx?)/,
            system       : ~/.*(\.(?i)(bin|deb|lib|pkg|rpm)|(gcc.*))/,
            archive      : ~/.*\.(?i)(arc|gz|rar|zip|tar.gz|zip)/,
            web          : ~/.*(?i)(html?)/,
            instructions : ~'(?i).*(adoc|readme.*|md)',
            techDev      : ~/.*(?i)(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,
            config       : ~/.*(?i)(cfg|config.*|pem|properties|xml|yaml)/,
            data         : ~/.*(?i)(csv|jsonl?d?|lst|tab)/,
            media        : ~/.*(?i)(avi|jpe?g|ogg|mp3|mpe?g|wav)/,
            communication: ~/.*(?i)(eml|vcf)/,
    ]

    public static final Map<String, Map<String, Object>> DEFAULT_FILENAME_LOCATE = [
            (IGNORE) : [pattern: ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: IGNORE_BUNDLE],
            (PARSE)  : [pattern: ~/.*(?i)(accdb|docx?|go|groovy|gradle|jar|java|javascript|js|jsonl?d?|md|ods|odp|odt|php|pptx?|rtf|schema|sh|vsdx?|xlsx?)/, analysis: PARSE_BUNDLE],
            (TRACK): [pattern: null, analysis: TRACK_BUNDLE],
    ]

    public static final Map<String, Map<String, Object>> DEFAULT_FILENAME_PARSE = [
            (IGNORE) : [pattern: ~/(?i)([.~]*lock.*|_.*|.*\bte?mp|.*\.class|.*\.pem|skipme.*)/, analysis: IGNORE_BUNDLE],
            office       : [pattern: ~/.*\.(?i)(accdb|docx?|ods|odp|odt|pptx?|rtf|vsdx?|xlsx?)/,analysis: PARSE_BUNDLE],
            system       : [pattern: ~/.*(\.(?i)(bin|deb|lib|pkg|rpm)|(gcc.*))/,analysis: PARSE_BUNDLE],
            web          : [pattern: ~/.*(?i)(html?)/,analysis: PARSE_BUNDLE],
            instructions : [pattern: ~'(?i).*(adoc|readme.*|md)',analysis: PARSE_BUNDLE],
            techDev      : [pattern: ~/.*(?i)(c|css|go|groovy|gradle|jar|java|javascript|js|php|schema|sh)/,analysis: PARSE_BUNDLE],
            config       : [pattern: ~/.*(?i)(cfg|config.*|pem|properties|xml|yaml)/,analysis: PARSE_BUNDLE],
            data         : [pattern: ~/.*(?i)(csv|jsonl?d?|lst|tab)/,analysis: PARSE_BUNDLE],
            media        : [pattern: ~/.*(?i)(avi|jpe?g|ogg|mp3|mpe?g|wav)/,analysis: PARSE_BUNDLE],
            communication: [pattern: ~/.*(?i)(eml|vcf)/,analysis: PARSE_BUNDLE],
            (TRACK): [pattern: null, analysis: PARSE_BUNDLE],
    ]

    /** path oriented pattern, which will 'cascade' down the label through child folders */
    public static final Map<String, Pattern> DEFAULT_FOLDERPATH_MAP = [
            data : [pattern: ~/.*(\/data\/).*/, analysis: PARSE_BUNDLE],
            build: [pattern: ~/.*(\/build\/).*/, analysis: PARSE_BUNDLE],
            work : [pattern: ~/.*(\/data\/).*/, analysis: SOURCE_BUNDLE],
    ]

    public static final Map<String, Pattern> DEFAULT_FILEPATH_MAP = [
//            configuration: ~/.*(?i)(\/.local\/|configs)/,
//            data         : ~/.*(\/data\/).*/,
//            build           : ~/.*(\/build\/).*/,
work: ~/.*\/work\/.*/,
    ]

}
