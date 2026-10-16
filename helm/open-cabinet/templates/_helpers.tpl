{{- define "open-cabinet.fullname" -}}
{{-   .Chart.Name -}}
{{- end -}}

{{- define "server.fullname" -}}
{{-   printf "%s-server" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
