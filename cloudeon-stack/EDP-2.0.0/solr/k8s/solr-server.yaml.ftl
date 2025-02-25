<#if conf["data.path.list"]??&& conf["data.path.list"]?trim?has_content>
    <#assign primeDataPathList=conf["data.path.list"]?trim?split(",")>
<#else >
    <#assign primeDataPathList=[conf['global.persistence.basePath']]>
</#if>
<#assign dataPathList = []>
<#list primeDataPathList as dataPath>
    <#if dataPath?ends_with("/")>
      <#assign dataPathList = dataPathList + [dataPath+ roleFullName]>
    <#else>
      <#assign dataPathList = dataPathList + [dataPath+"/"+ roleFullName]>
    </#if>
</#list>
---
apiVersion: "apps/v1"
kind: "Deployment"
metadata:
  labels:
    name: "${roleServiceFullName}"
    sname: ${serviceFullName}
  name: "${roleServiceFullName}"
spec:
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: "${roleServiceFullName}"
      sname: ${serviceFullName}
  strategy:
    type: "RollingUpdate"
    rollingUpdate:
      maxSurge: 0
      maxUnavailable: 1
  minReadySeconds: 5
  revisionHistoryLimit: 10
  template:
    metadata:
      labels:
        name: "${roleServiceFullName}"
        app: "${roleServiceFullName}"
        podConflictName: "${roleServiceFullName}"
        sname: ${serviceFullName}
        inject-filebeat: "true"
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchLabels:
                name: "${roleServiceFullName}"
                podConflictName: "${roleServiceFullName}"
            topologyKey: "kubernetes.io/hostname"
      hostPID: false
      <#--Pod 中的容器将共享宿主机的网络接口、IP 地址和端口空间-->
      hostNetwork: true
      nodeSelector:
        ${roleServiceFullName}: "true"
      terminationGracePeriodSeconds: 30
      containers:
<#if dependencies.FILEBEAT??>
      - name: filebeat
        image: "${dependencies.FILEBEAT.conf['serverImage']}"
        imagePullPolicy: "${conf['global.imagePullPolicy']}"
    # 设置root用户运行，避免写入filebeat.registry失败
        securityContext:
          runAsUser: 0
        env:
          - name: NODE_NAME
            valueFrom:
              fieldRef:
                fieldPath: spec.nodeName
          - name: POD_NAME
            valueFrom:
              fieldRef:
                fieldPath: metadata.name
          - name: POD_IP
            valueFrom:
              fieldRef:
                fieldPath: status.podIP
    <#--禁用filebeat 检测，避免因filebeat状态异常影响安装-->
    <#--    readinessProbe:-->
    <#--      exec:-->
    <#--        command:-->
    <#--        - sh-->
    <#--        - -c-->
    <#--        - |-->
    <#--          #!/usr/bin/env bash -e-->
    <#--          filebeat test output-->
        resources:
          limits:
            cpu: "1"
            memory: 200Mi
          requests:
            cpu: 10m
            memory: 100Mi
        volumeMounts:
        - mountPath: /usr/share/filebeat/filebeat.yml
          name: filebeat-config
          subPath: filebeat.yml
        - mountPath: "/workspace/filebeat"
          name: "workspace"
          subPath: filebeat
        - mountPath: "/workspace/logs"
          name: "workspace"
          subPath: logs
</#if>
      - image: "${conf['serverImage']}"
        imagePullPolicy: "${conf['global.imagePullPolicy']}"
        name: "${roleServiceFullName}"
        command: ["/bin/bash","-c"]
        args:
          - |
            /bin/bash /opt/global/bootstrap.sh && \
            /bin/bash /opt/service-common/bootstrap.sh;
        readinessProbe:
          exec:
            command:
            - "/bin/bash"
            - "/opt/service-common/readiness.sh"
          failureThreshold: 3
          initialDelaySeconds: 10
          periodSeconds: 10
          successThreshold: 1
          timeoutSeconds: 5
        resources:
          requests:
            memory: "${conf['solr.container.request.memory']}Mi"
            cpu: "${conf['solr.container.request.cpu']}"
          limits:
            memory: "${conf['solr.container.limit.memory']}Mi"
            cpu: "${conf['solr.container.limit.cpu']}"
        env:
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        - name: MEM_LIMIT
          valueFrom:
            resourceFieldRef:
              resource: limits.memory
        - name: RENDER_TPL_DIR
          value: "/opt/service-render"
        - name: RENDER_MODEL
          value: "/opt/service-common/values.json"
        - name: ROLE_FULL_NAME
          value: "${roleFullName}"
        - name: SOLR_PORT
          value: "${conf["solr.port"]}"
        - name: SOLR_METRICS_PORT
          value: "${conf["solr.metrics.port"]}"
        - name: SOLR_HEAP
          value: "${conf["solr.heap"]}M"
        volumeMounts:
        - mountPath: "/etc/localtime"
          name: "timezone"
        - name: global-service-common
          mountPath: /opt/global/bootstrap.sh
          subPath: bootstrap.sh
        - name: global-render-config
          mountPath: /opt/global/10.render
        - name: global-usersync-config
          mountPath: /opt/global/20.usersync
        - name: global-copy-filebeat-config
          mountPath: /opt/global/30.copy-filebeat-config
        - name: service-render
          mountPath: /opt/service-render
        - name: service-common
          mountPath: /opt/service-common
        - mountPath: "/workspace"
          name: "workspace"
<#list dataPathList as dataPath>
        - name: local-data-${dataPath?index+1}
          mountPath: /data/${dataPath?index+1}
</#list>
      volumes:
      - hostPath:
          path: "/etc/localtime"
        name: "timezone"
      - name: global-service-common
        configMap:
          name: global-service-common
      - name: global-render-config
        configMap:
          name: global-render-config
      - name: global-usersync-config
        configMap:
          name: global-usersync-config
      - name: global-copy-filebeat-config
        configMap:
          name: global-copy-filebeat-config
      - name: service-render
        configMap:
          name: solr-service-render
      - name: service-common
        configMap:
          name: solr-service-common
      - name: "workspace"
        hostPath:
          type: DirectoryOrCreate
          path: "${dataPathList[0]}/workspace"
<#list dataPathList as dataPath>
      - name: local-data-${dataPath?index+1}
        hostPath:
          path: ${dataPath}/data
          type: DirectoryOrCreate
</#list>
<#if dependencies.FILEBEAT??>
      - configMap:
          name: filebeat-common-config
        name: filebeat-config
</#if>
