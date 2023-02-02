{{- define "common.require.value" }}
  {{ required "Please provide value for variable" .domain }}
{{- end }}