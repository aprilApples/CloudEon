apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: ranger-service-monitor
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
  name: metrics-ranger-admin
  labels:
    sname: ${serviceFullName}
    roleFullName: ranger-admin
spec:
  selector:
    sname: ${serviceFullName}
    roleFullName: ranger-admin
  ports:
  - name: metrics
    port: 9505
