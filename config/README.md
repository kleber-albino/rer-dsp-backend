# Configuração operacional do backend

Arquivos consumidos em runtime (`DSP_*_FILE` → `/config/` na imagem Docker).

- **Versionados:** baseline demo/quickstart (`installation-config.json`, `mapLayersConfig.json`, etc.) e templates `*.example`.
- **Gerados:** `./config.sh` no repositório `dsp-core` grava aqui a partir de `adopter-config.yaml` e copia `mapLayersConfig.json` / `downloadThemesConfig.json` para os jobs.

Build local: `docker build .` (não depende mais do `dsp-core`).
