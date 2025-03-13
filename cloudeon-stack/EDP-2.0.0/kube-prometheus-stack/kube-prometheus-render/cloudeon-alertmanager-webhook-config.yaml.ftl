apiVersion: monitoring.coreos.com/v1alpha1
kind: AlertmanagerConfig
metadata:
  name: server-alertmanager-config
<#--这里的命名空间是要监控的命名空间，而非kube-prometheus所在的命名空间-->
  namespace: ${namespace}
  labels:
    alertmanagerConfig: "true"
spec:
<#-- 这个route将匹配所有当前命名空间的告警，所以需要将默认receiver设置为null，并设置子路由-->
  route:
    <#--alertManager 按照命名空间和alertname分组-->
    groupBy: ['namespace','alertname']
    groupWait: 30s
    groupInterval: 1m
    repeatInterval: 2m
    receiver: 'null'
    continue: true
<#--    matchers:-->
<#--      - name: receiver-->
<#--        value: "webhook"-->
    routes:
      - receiver: 'web.hook'
        continue: true
        matchers:
          - name: receiver
            value: "webhook"
<#if conf['alertManagerConfig.sendEmail']?? && conf['alertManagerConfig.sendEmail'] == "true">
      - receiver: 'email'
        continue: true
        matchers:
          - name: sendEmail
            value: "true"
</#if>
  receivers:
  - name: 'null'
  - name: 'web.hook'
    webhookConfigs:
    - url: '${cloudeonURL}/apiPre/alert/webhook'
      sendResolved: true
<#if conf['alertManagerConfig.sendEmail']?? && conf['alertManagerConfig.sendEmail'] == "true">
  - name: 'email'
    emailConfigs:
    - to: '{{ template "email.to" . }}'
      html: '{{ template "email.content.html" .}}'
      from: "${conf['alertManagerConfig.from']}"
      smarthost: "${conf['alertManagerConfig.smarthost']}"
      authUsername: "${conf['alertManagerConfig.authUsername']}"
      authPassword:
        key: authPassword
        name: smtp-credentials
      requireTLS: ${conf['alertManagerConfig.requireTLS']}
      sendResolved: true
</#if>
  inhibitRules:
    - sourceMatch:
        - name: alertLevel
          value: "异常级别"
      targetMatch:
        - name: alertLevel
          value: "告警级别"
      equal: ['alert', 'dev', 'instance']
<#if conf['alertManagerConfig.sendEmail']?? && conf['alertManagerConfig.sendEmail'] == "true">
---
apiVersion: v1
kind: Secret
metadata:
  name: smtp-credentials
  namespace: ${namespace}
type: Opaque
data:
  authPassword: "${conf['alertManagerConfig.authPassword']}"
</#if>