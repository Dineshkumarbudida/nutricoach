.PHONY: core-test backend-test web-build verify compose-up compose-down backup restore

core-test:
	./backend/scripts/run-core-tests.sh

backend-test:
	cd backend && mvn test

web-build:
	cd web && npm install && npm run build

verify: core-test
	./scripts/verify.sh

compose-up:
	docker compose up --build

compose-down:
	docker compose down

backup:
	./scripts/backup.sh

restore:
	@test -n "$(FILE)" || (echo "Usage: make restore FILE=backups/file.dump" >&2; exit 2)
	ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh "$(FILE)"
