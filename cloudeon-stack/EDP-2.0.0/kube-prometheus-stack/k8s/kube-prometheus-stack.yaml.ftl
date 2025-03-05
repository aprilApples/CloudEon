apiVersion: helm.cattle.io/v1
kind: HelmChart
metadata:
  name: ${roleServiceFullName}
  namespace: ${conf['kube-prometheus.namespace']}
spec:
  repo: ${conf['kube-prometheus.helm.repo']}
  chart: kube-prometheus-stack
  targetNamespace: ${conf['kube-prometheus.namespace']}
  set:
<#macro property key value>
    ${key}: "${value}"
</#macro>
<#list confFiles['kube-prometheus-stack.conf'] as key, value>
    <@property key value/>
</#list>
  valuesContent: |-
    namespaceOverride: "${conf['kube-prometheus.namespace']}"
    prometheus:
      prometheusSpec:
<#--添加自定义抓取配置，用于抓取注解形式的指标-->
        additionalScrapeConfigsSecret:
          enabled: true
          name: "prometheus-additional-scrape-configs"
          key: "prometheus-additional.yaml"
<#--自动抓取所有命名空间的serviceMonitor、podMonitor、alertRule-->
        serviceMonitorSelectorNilUsesHelmValues: false
        podMonitorSelectorNilUsesHelmValues: false
        ruleSelectorNilUsesHelmValues: false
<#--不设置的话默认会屏蔽掉一些指标，导致与prometheus-operator/kube-prometheus的不一样-->
    kubelet:
      serviceMonitor:
        cAdvisorMetricRelabelings: ""
    grafana:
      image:
        registry: registry.cn-guangzhou.aliyuncs.com
        repository: bigdata200/grafana
        # Overrides the Grafana image tag whose default is the chart appVersion
        tag: "11.4.0"
        pullPolicy: IfNotPresent
      sidecar:
        dashboards:
          provider:
            allowUiUpdates: true
          folderAnnotation: "folder"
      grafana.ini:
        analytics:
          check_for_updates : false
        auth.anonymous:
          enabled : true
        security:
          allow_embedding: true
    alertmanager:
      templateFiles:
        email.tmpl: |-
          {{ define "email.to" }}{{ (index .Alerts 0).Labels.email }}{{ end }}
        content.tmpl: |-
          {{ define "email.content.html" }}
          <html>
          <body>
            {{- range .Alerts }}
            <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
              <h3 style="color: #d9534f;">🚨 告警通知</h3>
              <table style="width: 100%; border-collapse: collapse;">
                <tr>
                  <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>告警名称</strong></td>
                  <td style="padding: 8px; border: 1px solid #ddd;">{{ .Labels.alertname }}</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>严重级别</strong></td>
                  <td style="padding: 8px; border: 1px solid #ddd; color: #d9534f;">{{ .Labels.alertLevel }}</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>实例</strong></td>
                  <td style="padding: 8px; border: 1px solid #ddd; font-family: monospace;">{{ .Labels.instance }}</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>摘要</strong></td>
                  <td style="padding: 8px; border: 1px solid #ddd;">{{ .Annotations.alertInfo | html }}</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>建议</strong></td>
                  <td style="padding: 8px; border: 1px solid #ddd;">{{ .Annotations.alertAdvice | html }}</td>
                </tr>
                <tr>
                  <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>触发时间</strong></td>
                  <td style="padding: 8px; border: 1px solid #ddd;">{{ .StartsAt.Format "2006-01-02 15:04:05" }}</td>
                </tr>
              </table>
            </div>
            {{- end }}
          </body>
          </html>
          {{ end }}
      extraVolumes:
        - name: email-templates
          configMap:
            name: alertmanager-email-templates
      extraVolumeMounts:
        - name: email-templates
          mountPath: /etc/alertmanager/config/
      alertmanagerSpec:
        alertmanagerConfigMatcherStrategy:
          type: None
        alertmanagerConfigSelector:
          matchLabels:
            alertmanagerConfig: "true"
    prometheusOperator:
      admissionWebhooks:
        patch:
          enabled: true
          image:
            registry: ${conf['image.registry.proxy.k8s']}
            pullPolicy: IfNotPresent
    kube-state-metrics:
      image:
        registry: ${conf['image.registry.proxy.k8s']}
        pullPolicy: IfNotPresent
    additionalConfigMaps:
      - name: alertmanager-email-templates
        namespace: ${conf['kube-prometheus.namespace']}
        data:
          email.tmpl: |
            {{ define "email.to" }}{{ (index .Alerts 0).Labels.email }}{{ end }}
          content.tmpl: |
            {{ define "email.content.html" }}
            <html>
            <body>
              {{- range .Alerts }}
              <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
                <h3 style="color: #d9534f;">🚨 告警通知</h3>
                <table style="width: 100%; border-collapse: collapse;">
                  <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>告警名称</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">{{ .Labels.alertname }}</td>
                  </tr>
                  <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>严重级别</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd; color: #d9534f;">{{ .Labels.alertLevel }}</td>
                  </tr>
                  <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>实例</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd; font-family: monospace;">{{ .Labels.instance }}</td>
                  </tr>
                  <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>摘要</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">{{ .Annotations.alertInfo | html }}</td>
                  </tr>
                  <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>建议</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">{{ .Annotations.alertAdvice | html }}</td>
                  </tr>
                  <tr>
                    <td style="padding: 8px; border: 1px solid #ddd; background-color: #f9f9f9;"><strong>触发时间</strong></td>
                    <td style="padding: 8px; border: 1px solid #ddd;">{{ .StartsAt.Format "2006-01-02 15:04:05" }}</td>
                  </tr>
                </table>
              </div>
              {{- end }}
            </body>
            </html>
            {{ end }}