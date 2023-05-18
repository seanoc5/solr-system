package com.oconeco.helpers

// persistent issues with groovy moving/repacking CliBuilder. Hopefully this lasts for a while -- SoC 20230517

import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import org.apache.log4j.Logger

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
    static final Logger log = Logger.getLogger(this.class.name)

    public static ConfigObject parse(String toolName, String[] args) {
        CliBuilder cli = new CliBuilder(usage: "${toolName}.groovy --config=configLocate.groovy", width: 160)
        cli.with {
            h longOpt: 'help', 'Show usage information'
            c longOpt: 'config', args: 1, required: true, argName: 'ConfigFile', 'optional configuration file to be parsed/slurped with configSlurper'
            f longOpt: 'folders', 'Parent folder(s) to crawl' , args: '+', valueSeparator: ',', required: false
            n longOpt: 'name', args:1, required: false, 'Name of crawl to be added to all solr input docs (files and folders)'
            s longOpt: 'solrUrl', args: 1, required: false, 'Solr url with protocol, host, and port (if any)'
            w longOpt: 'wipeContent', required: false, 'Clear data from previous crawl (beware--easy to really bork things with this..)'
        }

        OptionAccessor options = cli.parse(args)
        if (!options) System.exit(-1)

        if (options.help) {
            cli.usage()
            System.exit(0)
        }

        String configLocation = options.config
        File cfgFile = new File(configLocation)

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        ConfigObject config = null

        def resourcesRoot = cl.getResource('.')
        log.info "\t\tStarting with resources root: $resourcesRoot"
        if (cfgFile.exists()) {
            log.info "\t\tcfg file: ${cfgFile.absolutePath}"
        } else {
            log.info "\t\tcould not find config file: ${cfgFile.absolutePath}, trying resource path: $resourcesRoot"
            URL cfgUrl = cl.getResource(configLocation)
            if (cfgUrl) {
                cfgFile = new File(cfgUrl.toURI())
                if (cfgFile.exists()) {
                    log.info "\t\tFound config file (${configLocation}) in resources: ${cfgFile.absolutePath}"
                    config = new ConfigSlurper().parse(cfgFile.toURI().toURL())
                } else {
                    throw new IllegalArgumentException("Config file: $cfgFile does not exist, bailing...")
                }
            } else {
                log.warn "No config url found?? configLocation:$configLocation "
                throw new IllegalArgumentException("No config url found in args, exiting!!!")
            }
        }


        if (options.solrUrl) {
            log.info "\t\tUsing Solr url from Command line (overriding config file): ${options.solrUrl}"
            config.solrUrl = options.solrUrl
        } else if(config.solrUrl){
            log.info "\t\tUsing Solr url from CONFIG file): ${config.solrUrl}"
        } else {
            log.info "\t\tNo solr url given in command line args NOR config file, this should not be possible, exiting..."
            System.exit(-2)
        }

        def localFolders = config.dataSources.localFolders
        def folders = options.folderss
        if(options.folderss){
            List<String> ds = options.folderss
            int cnt = ds.size()
            Map dsMap = ds.collectEntries{it.split(':')}
            log.info "\t\tReplace config folders ($localFolders) with folders from command line args: ($dsMap) "
            config.dataSources.localFolders = dsMap
        }

        def wipe = options.wipeContent
        if(wipe){
            log.warn "Found flag `wipeContent` which will send deletes for each datasource"
            config.wipeContent = true
        } else if(config.wipeContent){
            log.warn "Found config file setting `wipeContent` with value (${config.wipeContent}) which might send deletes for each datasource"
        }

        return config
    }

}
