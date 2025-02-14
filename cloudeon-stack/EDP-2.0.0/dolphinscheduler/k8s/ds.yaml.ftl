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
    sname: "${serviceFullName}"
    roleFullName: "${roleFullName}"
  name: "${roleServiceFullName}"
spec:
  replicas: ${roleNodeCnt}
  selector:
    matchLabels:
      app: "${roleServiceFullName}"
      sname: "${serviceFullName}"
      roleFullName: "${roleFullName}"
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
        sname: "${serviceFullName}"
        roleFullName: "${roleFullName}"
        app: "${roleServiceFullName}"
        podConflictName: "${roleServiceFullName}"
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
      - name: "server"
        imagePullPolicy: "${conf['global.imagePullPolicy']}"
        image: "${conf['serverImage']}"
        command: ["/bin/bash","-c"]
        args:
          - |
            /bin/bash /opt/global/bootstrap.sh && \
            /bin/bash /opt/service-common/bootstrap.sh;
<#switch roleFullName>
<#case "ds-master">
        readinessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: ${conf['master.server.port']}
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 1
        resources:
          requests:
            memory: "${conf['ds.master.container.request.memory']}Mi"
            cpu: "${conf['ds.master.container.request.cpu']}"
          limits:
            memory: "${conf['ds.master.container.limit.memory']}Mi"
            cpu: "${conf['ds.master.container.limit.cpu']}"
<#break>
<#case "ds-worker">
        readinessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: ${conf['worker.server.port']}
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 1
        resources:
          requests:
            memory: "${conf['ds.worker.container.request.memory']}Mi"
            cpu: "${conf['ds.worker.container.request.cpu']}"
          limits:
            memory: "${conf['ds.worker.container.limit.memory']}Mi"
            cpu: "${conf['ds.worker.container.limit.cpu']}"
<#break>
<#case "ds-api">
        readinessProbe:
          httpGet:
            path: /dolphinscheduler/actuator/health/liveness
            port: ${conf['api.server.port']}
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 1
        resources:
          requests:
            memory: "${conf['ds.api.container.request.memory']}Mi"
            cpu: "${conf['ds.api.container.request.cpu']}"
          limits:
            memory: "${conf['ds.api.container.limit.memory']}Mi"
            cpu: "${conf['ds.api.container.limit.cpu']}"
<#break>
<#case "ds-alert">
        readinessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: ${conf['alert.server.port']}
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 1
        resources:
          requests:
            memory: "${conf['ds.alert.container.request.memory']}Mi"
            cpu: "${conf['ds.alert.container.request.cpu']}"
          limits:
            memory: "${conf['ds.alert.container.limit.memory']}Mi"
            cpu: "${conf['ds.alert.container.limit.cpu']}"
<#break>
</#switch>
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
          - name: SEATUNNEL_HOME
            value: "/opt/seatunnel"
          - name: HADOOP_CLASSPATH
            value: "/opt/hadoop/etc/hadoop:/opt/hadoop/share/hadoop/common/lib/*:/opt/hadoop/share/hadoop/common/*:/opt/hadoop/share/hadoop/hdfs:/opt/hadoop/share/hadoop/hdfs/lib/*:/opt/hadoop/share/hadoop/hdfs/*:/opt/hadoop/share/hadoop/mapreduce/*:/opt/hadoop/share/hadoop/yarn:/opt/hadoop/share/hadoop/yarn/lib/*:/opt/hadoop/share/hadoop/yarn/*"
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
        - name: hdfs-config
          mountPath: /etc/hdfs-config
        - name: yarn-config
          mountPath: /etc/yarn-config
<#if dependencies.FLINK??>
        - name: flink-config
          mountPath: /etc/flink-config
</#if>
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
          name: dolphinscheduler-service-render
      - name: service-common
        configMap:
          name: dolphinscheduler-service-common
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
      - name: hdfs-config
        configMap:
          name: hdfs-config
      - name: yarn-config
        configMap:
          name: yarn-config
<#if dependencies.FLINK??>
      - configMap:
          name: flink-config
        name: flink-config
</#if>

