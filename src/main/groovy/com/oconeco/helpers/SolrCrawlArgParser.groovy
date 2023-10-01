package com.oconeco.helpers

// persistent issues with groovy moving/repacking CliBuilder. Hopefully this lasts for a while -- SoC 20230517

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger

//import groovy.cli.commons.CliBuilder
//import groovy.cli.picocli.OptionAccessor
//import groovy.cli.picocli.CliBuilder
//import groovy.cli.commons.CliBuilder
//import groovy.cli.commons.OptionAccessor

/**
 * @author :    sean
 * @mailto :    seanoc5@gmail.com
 * @created :   7/31/22, Sunday
 * @description:
 */

/**
 * General helper class trying to standardize args specific to solr operations
 * probably can be deprecated or removed, but leaving for now
 */
class SolrCrawlArgParser {
    static final Logger log = LogManager.getLogger(this.class.name)

    public static ConfigObject parse(String toolName, String[] args) {
        CliBuilder cli = new CliBuilder(usage: "${toolName}.groovy --config=configLocate.groovy", width: 160)
        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', required: true, args: 1, argName: 'ConfigFile', 'optional configuration file to be parsed/slurped with configSlurper'
            f longOpt: 'folders', required: false, 'Parent folder(s) to crawl', args: '+', valueSeparator: ','
            l longOpt: 'locationName', args: 1, required: false, 'Name of source location to be added to all solr input docs (files and folders), i.e. hostname, profile name, imap account,...'
            s longOpt: 'solrUrl', args: 1, required: false, 'Solr url with protocol, host, and port (if any)'
            w longOpt: 'wipeContent', required: false, 'Clear data from previous crawl (beware--easy to really bork things with this..)'
        }

        OptionAccessor options = cli.parse(args)
        if (!options) System.exit(-1)

        if (options.help) {
            cli.usage()
            System.exit(0)
        }

        def foo = this.getClassLoader().getResource('/configs/configCrawl.groovy')
        def fooDot = this.getClassLoader().getResource('.')
        def bar = this.class.getResource('/')
        def barDot = this.getResource('.')

        String configLocation = options.config
        if (configLocation) {
            log.info "Config location from commandline args: $configLocation"
        } else {
            configLocation = '/configs/configCrawl.groovy'
            log.warn "No Config location given as CLI arg, setting to default: $configLocation"
        }
        URL cfgUrl = SolrCrawlArgParser.class.getResource(configLocation)
        File cfgFile = new File(cfgUrl.toURI())
        if (cfgFile.exists()&&cfgFile.canRead()){
            log.debug "good, cfg file exists and is readable"
        } else {
            throw new IllegalArgumentException("No valid config file(${cfgFile.absolutePath} found/readable, bailing with forced exception...")
        }

        ConfigObject config = null
        if (cfgFile.exists()) {
            log.info "\t\tcfg file: ${cfgFile.absolutePath}"
            config = new ConfigSlurper().parse(cfgUrl)
        } else {
            log.warn "\t\tcould not find config file: ${cfgFile.absolutePath}"
            cli.usage()
            System.exit(0)
//            URL cfgUrl = cl.getResource(configLocation)
        }

        // -------------------------- Solr Server URL --------------------------
        String solrArg = options.solrUrl
        if (options.solrUrl) {
            if (solrArg.endsWith('solr')) {
//                config.solrUrl = solrArg + '/' + Constants.DEFAULT_COLL_NAME
                config.solrUrl = solrArg
                log.info "\t\tUsing Solr url from Command line (overriding config file): ${options.solrUrl} (NOTE: added default app name:(${Constants.DEFAULT_COLL_NAME})"
            } else if(config.solrUrl.endsWtith('solr/')){
                config.solrUrl = solrArg
//                config.solrUrl = solrArg + Constants.DEFAULT_COLL_NAME

                log.info "\t\tGood, we have a base solr url:($solrArg), this allows switching collections "
//            } else if (solrArg.endsWith('solr/')) {
//                config.solrUrl = solrArg + Constants.DEFAULT_COLL_NAME
//                log.info "\t\tUsing Solr url from Command line (overriding config file): ${options.solrUrl} (NOTE: added default app name:(${Constants.DEFAULT_COLL_NAME})"
            } else {
                log.info "\t\tUsing Solr url from Command line (overriding config file): ${options.solrUrl}"
                config.solrUrl = options.solrUrl
            }
        } else if (config?.solrUrl) {
            log.info "\t\tUsing Solr url from CONFIG file): ${config.solrUrl}"
        } else {
            log.info "\t\tNo solr url given in command line args NOR config file, this should not be possible, exiting..."
            System.exit(-2)
        }

        // -------------------------- DEFAULT COLLECTION --------------------------
        String argDefaultCollection = options.defaultCollection
        if (argDefaultCollection && argDefaultCollection!='false') {
            log.info "\t\tFound default solr collection($argDefaultCollection) in command line options, overrides anything from config file :-)"
            config.defaultCollection = argDefaultCollection
        } else if (config.solrDefaultCollection) {
            log.info "Found default solr collection(${config.solrDefaultCollection}) in config file(${configLocation}), using that :-)"
        } else {
            config.solrDefaultCollection = Constants.DEFAULT_COLL_NAME
            log.warn "No default solr collection found in config file ($configLocation) nor Command line args, setting to hard coded (Constants.groovy) collection:(${Constants.DEFAULT_COLL_NAME})"
        }

        // -------------------------- get local folders to crawl (can be overridden by cl args --------------------------
        def localFolders = config?.dataSources.localFolders
        if (options.folderss) {
            List<String> ds = options.folderss
            int cnt = ds.size()
            Map dsMap = ds.collectEntries { String s ->
                int pos = s.indexOf(':')
                if (pos) {
                    String label = s[0..pos - 1]
                    String path = s[pos + 1..-1]
                    return [(label): path]
                } else {
                    log.warn "Cannot parse localfolder command line arg:($s) -- should be: -flabel:/some/path/here"
                }
            }
            log.info "\t\tUsing command line folder args: ($dsMap) to verride config localfolders ($localFolders) "
            config?.dataSources.localFolders = dsMap
        }

        def wipe = options.wipeContent
        if (wipe) {
            log.warn "\t\tFound flag `wipeContent` which will send deletes for each datasource"
            config?.wipeContent = true
        } else if (config.wipeContent) {
            log.warn "Found config file setting `wipeContent` with value (${config.wipeContent}) which might send deletes for each datasource"
        }

        return config
    }

}
