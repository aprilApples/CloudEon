<#--handle dependent.zookeeper-->
<#if dependencies.ZOOKEEPER??>
    <#assign zookeeper=dependencies.ZOOKEEPER quorum=[]>
    <#list zookeeper.serviceRoles['ZOOKEEPER_SERVER'] as role>
        <#assign quorum += [role.hostname + ":" + zookeeper.conf["zookeeper.client.port"]]>
    </#list>
</#if>
export ZOOKEEPER_CLUSTER_HOST=${quorum?join(",")}