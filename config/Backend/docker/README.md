# Backend Docker

The image is built from this repository's `Dockerfile`.

Operational JSON and About content are copied from `config/` into `/config` at
build time via `config/docker/select-runtime-config.sh`.

After `./config.sh` in **dsp-core**, rebuild the backend image (`./setup.sh` or
`./start.sh` in the core repo).
