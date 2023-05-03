package com.oconeco.helpers


import groovy.cli.picocli.CliBuilder
import groovy.cli.picocli.OptionAccessor
import org.apache.log4j.Logger

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

    public ConfigObject parse(String toolName, String[] args) {
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
        def resourcesRoot = getClass().getResource('/')
        log.info "Starting with resources root: $resourcesRoot"
        if (!cfgFile.exists()) {
            URL cfgUrl = getClass().getResource(configLocation)
            log.warn "could not find config file: ${cfgFile.absolutePath}, trying resource path: $cfgUrl"
            if (cfgUrl) {
                cfgFile = new File(cfgUrl.toURI())
                if (cfgFile.exists()) {
                    log.info "Found config file (${configLocation}) in resources: ${cfgFile.absolutePath}"
                } else {
                    throw new IllegalArgumentException("Config file: $cfgFile does not exist, bailing...")
                }
            } else {
                log.warn "No config url found?? "
            }
        } else {
            log.info "cfg file: ${cfgFile.absolutePath}"
        }
        ConfigObject config = new ConfigSlurper().parse(cfgFile.toURI().toURL())

        def solrUrlFromCfg = config.solrUrl
        if(options.solrUrl) {
            log.info "Solr url from cli: '${options.solrUrl}' -- replace config file url ($solrUrlFromCfg) with cli arg"
            config.solrUrl = options.solrUrl
        } else {
            log.info "No solr url given in command line args, assuming this is set in the config file..."
        }

        def localhost = InetAddress.getLocalHost();
        log.info( "InetAddress: " + localhost );
        log.info( "\tHost Name: " + localhost.getHostName() );
        def hostNameCfg = config.hostName


        return config
    }

}
