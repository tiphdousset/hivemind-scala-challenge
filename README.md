# Hivemind Scala Challenge

## Build
Requirements: sbt, docker

```
$ sbt docker:publishLocal
```

Ignore the informational log messages output as "error".

## Run
To import reviews from file (will truncate current reviews db):

(NOTE: reviews json file must reside in `test-data/`, as this directory is bind-mounted via Docker)

```
$ REVIEWS=test-data/reviews.json docker compose up
```

To run with previously-populated reviews db:

```
$ docker compose up
```

## Test

```
$ docker compose -f docker-development.yml up -d
$ docker ps
$ docker attach <mozilla/sbt container ID>
```

Once in the SBT console:

```
sbt:scalachallenge> test
```
