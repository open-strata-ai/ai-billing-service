REPO    := ai-billing-service
VERSION := 1.4.0

.PHONY: build test lint run generate

build:   ## compile
	mvn -q -B package -DskipTests

test:    ## run tests
	mvn -q -B test

lint:    ## static analysis
	mvn -q -B spotless:check

run:     ## run locally
	mvn -q -B spring-boot:run

generate: ## render doc/code skeletons
	python3 ../openstrata-meta/template/generate_app_skeletons.py
