# dockhand

[![Build Status](https://travis-ci.org/boxboat/dockhand.svg?branch=master)](https://travis-ci.org/boxboat/dockhand)

The Dockhand Jenkins Shared Library is a Groovy library meant to be used by any build pipelines that need to build images, push them to a remote registry, and trigger deployments to kubernetes clusters with Helm. The library encapsulates build functionality such as:

 - configuring git and checking out the repo to a build workspace
 - providing a wrapper to docker-compose to handle errors during docker-compose build, docker-compose up and docker-compose down when building and testing images
 - automatically updating the build-versions repo upon successful image builds
 - injection of secrets from Vault or AWS Secrets Manager using dockcmd into Helm Charts at deployment time
 - Handling automatic deployments of Helm charts upon commit to it’s corresponding application repos

`dockhand` is an upstream [Jenkins Shared Library](https://jenkins.io/doc/book/pipeline/shared-libraries/) to Build, Promote, and Deploy Docker images. 

## Getting Started

1. Fork the project
2. Copy `config.example.yaml` to `config.yaml` in [resources/com/boxboat/jenkins/](resources/com/boxboat/jenkins/)
3. Adjust [global config](docs/config/global.md) values

## Documentation

See [docs/README.md](docs/README.md)

## Quick Links

- Global configuration directory: [resources/com/boxboat/jenkins/](resources/com/boxboat/jenkins/)
- Global properties directory: [vars/](vars/)
