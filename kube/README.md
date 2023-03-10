# Deploying the app to Kubernetes

The application is ready to be deployed to a k8s cluster. The configuration
includes a Postgres service backed by a PersistentVolumeClaim, as well as
the application itself.

## Prerequisites

First you need to install [Kubernetes](https://kubernetes.io/docs/tasks/tools/install-kubectl-macos/)
and [minikube](https://minikube.sigs.k8s.io/docs/start/) (local Kubernetes for development).
Start minikube with `minikube start` and (optionally) launch its dashboard
with `minikube dashboard` (this allows observing all Kubernetes resources
along with their current status and latest events).

The Docker image for the application lives in Github Container Registry
in a private repository; in order to access it, you need to ask permissions
from the project's author. Then you will be able to copy `ghcr_creds.yml.example`
in this directory to `ghcr_creds.yml` and put your base64-encoded
`~/.docker/config.json` there.

## Running

Project configuration contains of `ghcr_creds.yml` file mentioned earlier
(contains credentials for GHCR) and `kube.yml` (contains everything else:
`Service`'s and `Deployment`'s for Postgres and the application, a `Secret` with
Postgres password and a `PersistentVolumeClaim` for Postgres data storage).

You can launch everything with:

``` sh
$ kubectl apply -f ghcr_creds.yml -f kube.yml
```

Then you can run `minikube service app --url` to get a URL of the application.

Stopping everything is as simple as

``` sh
$ kubectl delete -f kube.yml -f ghcr_creds.yml
```

*Note that this command also deletes the volume containing Postgres data.*
