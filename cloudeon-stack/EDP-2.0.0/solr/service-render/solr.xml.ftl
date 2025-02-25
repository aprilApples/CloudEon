<?xml version="1.0" encoding="UTF-8"?>
<#-- Define a macro for property elements -->
<#macro property key value>
<property>
    <name>${key}</name>
    <value>${value}</value>
</property>
</#macro>

<#macro createProperty key value>
<#if value == "true" || value == "false">
    <bool name="${key}">${value}</bool>
<#elseif value?is_number>
    <int name="${key}">${value}</int>
<#elseif value?matches("^[0-9]+$")>
    <int name="${key}">${value}</int>
<#else>
    <str name="${key}">${value}</str>
</#if>
</#macro>

<solr>
    <solrcloud>
        <str name="host"></str>
        <int name="hostPort">${conf['solr.port']}</int>
        <#--handle dependent.zookeeper-->
        <#if dependencies.ZOOKEEPER??>
            <#assign zookeeper=dependencies.ZOOKEEPER quorum=[]>
            <#list zookeeper.serviceRoles['ZOOKEEPER_SERVER'] as role>
                <#assign quorum += [role.hostname + ":" + zookeeper.conf["zookeeper.client.port"]]>
            </#list>
        </#if>
        <str name="zkHost">${quorum?join(",")}</str>


        <#list confFiles['solr.xml'] as key, value>
            <#if key?starts_with("solr.solrcloud.")>
                <#assign solrCloudKey = key?substring(15) />
                <@createProperty solrCloudKey value/>
            </#if>
        </#list>
    </solrcloud>

    <shardHandlerFactory name="shardHandlerFactory" class="HttpShardHandlerFactory">
        <#list confFiles['solr.xml'] as key, value>
            <#if key?starts_with("solr.shardHandlerFactory.")>
            <#-- 如果是，则去除 "solr.cloud." 前缀 -->
                <#assign shardHandlerFactoryKey = key?substring(25) />
                <@createProperty shardHandlerFactoryKey value/>
            </#if>
        </#list>
    </shardHandlerFactory>

    <#--自定义字段信息-->
    <#list confFiles['solr.xml'] as key, value>
        <#if !key?starts_with("solr.shardHandlerFactory.") && !key?starts_with("solr.solrcloud.")>
            <#if key == "metrics">
                <metrics enabled="${value}"/>
            <#else>
                <@createProperty key value/>
            </#if>
        </#if>
    </#list>
</solr>
