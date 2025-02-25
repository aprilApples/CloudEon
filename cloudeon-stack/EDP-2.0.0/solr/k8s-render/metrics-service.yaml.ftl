apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: solr-service-monitor
  labels:
    team: frontend
spec:
  selector:
    matchLabels:
      sname: ${serviceFullName}
  endpoints:
  - port: metrics
    path: /actuator/prometheus
---
kind: Service
apiVersion: v1
metadata:
  name: metrics-solr
  labels:
    sname: ${serviceFullName}
spec:
  selector:
    sname: ${serviceFullName}
  ports:
  - name: metrics
    port: ${conf['solr.metrics.port']}