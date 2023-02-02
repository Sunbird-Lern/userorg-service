{{- define "common.read.secret" -}}
{{- $secret := (lookup "v1" "Secret" .Namespace .Name).data -}}
{{- if $secret -}}
  {{- if hasKey $secret .Key -}}
    {{- index $secret .Key | b64dec -}}
  {{- else -}}
    {{- if .LocalDevelopment -}}
      {{- printf "ERROR | %s | The secret \"%s\" does not contain the key \"%s\" in namespace \"%s\"" .ChartName .Name .Key .Namespace -}}
    {{- else -}}
      {{- printf "ERROR | %s | The secret \"%s\" does not contain the key \"%s\" in namespace \"%s\"" .ChartName .Name .Key .Namespace | fail -}}
    {{- end -}}
  {{- end -}}
{{ else -}}
  {{- if .LocalDevelopment -}}
    {{- printf "ERROR | %s | The secret \"%s\" does not exist in the namespace \"%s\"" .ChartName .Name .Namespace -}}
  {{- else -}}
    {{- printf "ERROR | %s | The secret \"%s\" does not exist in the namespace \"%s\" %s" .ChartName .Name .Namespace .LocalDevelopment | fail -}}
  {{- end -}}
{{- end -}}
{{- end -}}