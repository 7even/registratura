# Registratura [![CI/CD](https://github.com/7even/registratura/actions/workflows/ci_cd.yml/badge.svg)](https://github.com/7even/registratura/actions/workflows/ci_cd.yml)

Registratura is a web application for managing a list of patients. It allows
viewing the list of patients (with the ability to filter by gender and age
as well as the fulltext search feature), creating new patients, updating and
deleting existing patients.

## Installation

### Database

The application uses PostgreSQL to store and retrieve the patients data.
By default it uses database called `registratura` at `localhost:5432`
under user that is equal to current shell user (`USER` env variable).

Database is migrated automatically while the application is starting up -
you only need to run `createdb registratura` to create the database.

### Development

Registratura is supposed to be developed via Emacs, so the repository includes
a `.dir-locals.el` file which makes application launch just by using
`M-x cider-jack-in-clj&cljs`.

The only prerequisites are Clojure's [tools.deps](https://clojure.org/guides/deps_and_cli)
(can be installed via `brew install clojure`) and
[shadow-cljs](https://github.com/thheller/shadow-cljs)
(`npm install -g shadow-cljs`). NPM dependencies should also be installed
before running the app (`npm install` from the project root).

`cider-jack-in-clj&cljs` automatically starts the backend and
the shadow-cljs watcher for the frontend code - just head
to [localhost:8888](http://localhost:8888/).

### Jar

The application can be compiled into a Jar file in 2 steps:

1. assemble a release version of the frontend app with `shadow-cljs release :main`
2. compile Clojure code and create a Jar file with `clj -T:build uberjar`

The file will be created at `target/registratura.jar` and can be launched
with `java -jar target/registratura.jar`.

*Note: compiling the Jar file assumes you already installed the prerequisites
from the previous section.*

### Docker

The repository includes a Dockerfile to create a Docker image containing
the application. The process is straightforward: `docker build -t registratura .`

A GitHub action automatically runs the application tests, builds a Docker image
and sends it to GitHub Container Registry on each push to the repository;
so instead of building the image yourself you can get it by running
`docker pull ghcr.io/7even/registratura:latest` (given you have the permissions).

## Configuration

Some aspects of the application can be configured using environment variables:

| Variable name | Description             | Default value      |
| ------------- | ----------------------- | ------------------ |
| `DB_HOST`     | Postgres server host    | `"localhost"`      |
| `DB_NAME`     | Postgres database name  | `"registratura"`   |
| `DB_USER`     | Postgres user name      | current shell user |
| `DB_PASSWORD` | Postgres password       | -                  |
| `HTTP_PORT`   | HTTP port of the server | `8888`             |

## Testing

Tests for both backend and frontend code can be run with `clj -M:clj:cljs:test`.
Be sure to `createdb registratura_test` first - it is used in the backend tests.
