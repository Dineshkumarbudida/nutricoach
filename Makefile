.PHONY: core-test backend-test web-build verify init-local init-lan init-https deploy-local deploy-lan deploy-https health-local health-lan health-https compose-down backup backup-rotate restore

core-test:
	./backend/scripts/run-core-tests.sh

backend-test:
	cd backend && mvn test

web-build:
	cd web && npm install && npm run build

verify: core-test
	./scripts/verify.sh

init-local:
	./scripts/init-deployment.sh local

init-lan:
	./scripts/init-deployment.sh lan

init-https:
	./scripts/init-deployment.sh https

deploy-local:
	./scripts/deploy.sh local

deploy-lan:
	./scripts/deploy.sh lan

deploy-https:
	./scripts/deploy.sh https

health-local:
	./scripts/healthcheck.sh local

health-lan:
	./scripts/healthcheck.sh lan

health-https:
	./scripts/healthcheck.sh https

compose-down:
	docker compose down

backup:
	./scripts/backup.sh

backup-rotate:
	./scripts/backup-rotate.sh

restore:
	@test -n "$(FILE)" || (echo "Usage: make restore FILE=backups/file.dump" >&2; exit 2)
	ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh "$(FILE)"
